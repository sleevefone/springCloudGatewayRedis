//// (这是一个新文件，用于替换 RedisRouteDefinitionRepository)
//package com.ocft.gateway.openapi.admin;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.ocft.gateway.openapi.config.RedisRouteDefinitionRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
//import org.springframework.cloud.gateway.filter.FilterDefinition;
//import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
//import org.springframework.cloud.gateway.route.RouteDefinition;
//import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
//import org.springframework.stereotype.Repository;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.net.URI;
//import java.util.List;
//
//@Repository
//@Primary // 声明这是主要的 RouteDefinitionRepository 实现
//@Slf4j
//@RequiredArgsConstructor
//public class DatabaseRouteDefinitionRepository implements RouteDefinitionRepository {
//
//    private final RouteDefinitionJpaRepository jpaRepository;
//    private final ObjectMapper objectMapper;
//    private final ReactiveStringRedisTemplate redisTemplate;
//    private final ApplicationEventPublisher eventPublisher;
//
//    @Override
//    public Flux<RouteDefinition> getRouteDefinitions() {
//        log.debug("Loading routes from database.");
//        // 注意：JPA 操作是阻塞的，在响应式流程中需要调度到合适的线程池
//        return Mono.fromCallable(jpaRepository::findAll)
//                .flatMapMany(Flux::fromIterable)
//                .map(this::convertToRouteDefinition);
//    }
//
//    @Override
//    public Mono<Void> save(Mono<RouteDefinition> routeDefinitionMono) {
//        return routeDefinitionMono.flatMap(routeDefinition ->
//                Mono.fromRunnable(() -> {
//                    jpaRepository.save(convertToEntity(routeDefinition));
//                    notifyChanged();
//                })
//        ).then();
//    }
//
//    @Override
//    public Mono<Void> delete(Mono<String> routeIdMono) {
//        return routeIdMono.flatMap(routeId ->
//                Mono.fromRunnable(() -> {
//                    jpaRepository.deleteById(routeId);
//                    notifyChanged();
//                })
//        ).then();
//    }
//
//    private void notifyChanged() {
//        // 通知机制保持不变！仍然使用 Redis Pub/Sub
//        redisTemplate.convertAndSend(RedisRouteDefinitionRepository.REFRESH_ROUTES_CHANNEL, "refresh")
//                .subscribe(
//                        null,
//                        error -> log.error("Failed to publish route refresh message to Redis.", error)
//                );
//        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
//    }
//
//    @SneakyThrows // Lombok 注解，简化 try-catch
//    private RouteDefinitionEntity convertToEntity(RouteDefinition rd) {
//        var entity = new RouteDefinitionEntity();
//        entity.setId(rd.getId());
//        entity.setUri(rd.getUri().toString());
//        entity.setRouteOrder(rd.getOrder());
//        entity.setPredicates(objectMapper.writeValueAsString(rd.getPredicates()));
//        entity.setFilters(objectMapper.writeValueAsString(rd.getFilters()));
//        return entity;
//    }
//
//    @SneakyThrows
//    private RouteDefinition convertToRouteDefinition(RouteDefinitionEntity entity) {
//        var rd = new RouteDefinition();
//        rd.setId(entity.getId());
//        rd.setUri(URI.create(entity.getUri()));
//        rd.setOrder(entity.getRouteOrder());
//
//        TypeReference<List<PredicateDefinition>> predicateType = new TypeReference<>() {};
//        rd.setPredicates(objectMapper.readValue(entity.getPredicates(), predicateType));
//
//        TypeReference<List<FilterDefinition>> filterType = new TypeReference<>() {};
//        rd.setFilters(objectMapper.readValue(entity.getFilters(), filterType));
//
//        return rd;
//    }
//}