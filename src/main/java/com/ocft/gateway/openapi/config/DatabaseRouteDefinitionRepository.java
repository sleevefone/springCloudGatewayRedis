package com.ocft.gateway.openapi.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocft.gateway.openapi.admin.GatewayFilterService;
import com.ocft.gateway.openapi.admin.RouteDefinitionEntity;
import com.ocft.gateway.openapi.admin.RouteDefinitionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class DatabaseRouteDefinitionRepository implements RouteDefinitionRepository {
    private static final String ROUTES_KEY = "gateway:routes";
    public static final String REFRESH_ROUTES_CHANNEL = "gateway:routes:refresh";
    private final RouteDefinitionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final GatewayFilterService gatewayFilterService;

    @Value("${gateway.routes.db.enabled:true}")
    private boolean dbRoutesEnabled;

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        if (!dbRoutesEnabled) {
            log.info("Loading routes from database is disabled by configuration 'gateway.routes.db.enabled'.");
            return Flux.empty();
        }
        List<String> filters = gatewayFilterService.getAvailableFilterNames();
        List<String> predicates = gatewayFilterService.getAvailablePredicateNames();
        log.debug("Loading active routes from database.");
        // JPA 是阻塞 IO，必须在专用的弹性线程池上执行，以避免阻塞 Netty 的事件循环线程
        // 只加载启用的路由 (enabled = true)，在数据库层面进行过滤以提高效率
        return Mono.fromCallable(() -> jpaRepository.findByEnabled(true))
                .flatMapMany(Flux::fromIterable)
                .map(this::convertToRouteDefinition)
                .filter(o->{
                    List<PredicateDefinition> predicates1 = o.getPredicates();
                    Set<String> collect = predicates1.stream().map(PredicateDefinition::getName).collect(Collectors.toSet());
                    collect.retainAll(predicates);
                    return !collect.isEmpty();
                }) .filter(o->{
                    List<FilterDefinition> filter = o.getFilters();
                    Set<String> filterSet = filter.stream().map(FilterDefinition::getName).collect(Collectors.toSet());
                    filterSet.retainAll(filters);
                    return !filterSet.isEmpty();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> routeDefinitionMono) {
        return routeDefinitionMono.flatMap(rd ->
                Mono.fromRunnable(() -> {
                    // 1. 先根据 ID 查找现有实体，以保留 enabled 等数据库特有的状态
                    RouteDefinitionEntity entity = jpaRepository.findById(rd.getId())
                            .orElseGet(() -> {
                                // 2. 如果不存在，则创建一个新实体。ID 来源于传入的 RouteDefinition。
                                // 新建的路由默认是启用的 (enabled=true)，由 RouteDefinitionEntity 的字段默认值保证。
                                RouteDefinitionEntity newEntity = new RouteDefinitionEntity();
                                newEntity.setId(rd.getId());
                                return newEntity;
                            });

                    // 3. 使用传入的 RouteDefinition 更新实体的属性
                    entity.setUri(rd.getUri().toString());
                    entity.setRouteOrder(rd.getOrder());
                    try {
                        entity.setPredicates(objectMapper.writeValueAsString(rd.getPredicates()));
                        entity.setFilters(objectMapper.writeValueAsString(rd.getFilters()));
                        List<String> filters = gatewayFilterService.getAvailableFilterNames();
                        Optional<Boolean> filterResult =  rd.getFilters().stream()
                                .map(predicateDefinition -> filters.contains(predicateDefinition.getName()))
                                .filter(tr -> !tr)
                                .findAny();
                        if (filterResult.orElse(false)){
                            entity.setFilterDescription("filter(s) not found");
                        }

                        List<String> predicates = gatewayFilterService.getAvailablePredicateNames();
                        Optional<Boolean> preResult =  rd.getPredicates().stream()
                                .map(predicateDefinition -> predicates.contains(predicateDefinition.getName()))
                                .filter(tr -> !tr)
                                .findAny();
                        if (preResult.orElse(false)){
                            entity.setFilterDescription("predicate(s) not found");
                        }
//                        log.info("save: {}",objectMapper.writeValueAsString(rd));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize predicates or filters for route {}", rd.getId(), e);
                        throw new RuntimeException("Error serializing route definition", e);
                    }

                    jpaRepository.save(entity);
                    notifyChanged();
                }).subscribeOn(Schedulers.boundedElastic())
        ).then();
    }

    @Override
    public Mono<Void> delete(Mono<String> routeIdMono) {
        return routeIdMono.flatMap(routeId ->
                Mono.fromRunnable(() -> {
                    jpaRepository.deleteById(routeId);
                    notifyChanged();
                }).subscribeOn(Schedulers.boundedElastic())
        ).then();
    }

    private void notifyChanged() {
        // 通知机制保持不变！仍然使用 Redis Pub/Sub
        redisTemplate.convertAndSend(REFRESH_ROUTES_CHANNEL, "refresh")
                .subscribe(null, error -> log.error("Failed to publish route refresh message to Redis.", error));
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }

    @SneakyThrows
    private RouteDefinition convertToRouteDefinition(RouteDefinitionEntity entity) {
        var rd = new RouteDefinition();
        rd.setId(entity.getId());
        rd.setUri(URI.create(entity.getUri()));
        rd.setOrder(entity.getRouteOrder());
        rd.setPredicates(objectMapper.readValue(entity.getPredicates(), new TypeReference<List<PredicateDefinition>>() {
        }));
        rd.setFilters(objectMapper.readValue(entity.getFilters(), new TypeReference<List<FilterDefinition>>() {
        }));
        log.info("entity =====: {}", entity);
        return rd;
    }
}