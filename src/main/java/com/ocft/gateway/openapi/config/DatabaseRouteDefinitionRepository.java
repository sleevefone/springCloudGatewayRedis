package com.ocft.gateway.openapi.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocft.gateway.openapi.admin.RouteDefinitionEntity;
import com.ocft.gateway.openapi.admin.RouteDefinitionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.List;

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

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        log.debug("Loading routes from database.");
        // JPA 是阻塞 IO，必须在专用的弹性线程池上执行，以避免阻塞 Netty 的事件循环线程
        return Mono.fromCallable(jpaRepository::findAll)
                .flatMapMany(Flux::fromIterable)
                .map(this::convertToRouteDefinition)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> save(Mono<RouteDefinition> routeDefinitionMono) {
        return routeDefinitionMono.flatMap(routeDefinition ->
                Mono.fromRunnable(() -> {
                    jpaRepository.save(convertToEntity(routeDefinition));
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
    private RouteDefinitionEntity convertToEntity(RouteDefinition rd) {
        var entity = new RouteDefinitionEntity();
        entity.setId(rd.getId());
        entity.setUri(rd.getUri().toString());
        entity.setRouteOrder(rd.getOrder());
        entity.setPredicates(objectMapper.writeValueAsString(rd.getPredicates()));
        entity.setFilters(objectMapper.writeValueAsString(rd.getFilters()));
        return entity;
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
        return rd;
    }
}