package com.ocft.gateway.openapi.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocft.gateway.openapi.config.RedisRouteDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "gateway.admin", name = "enabled", havingValue = "true")
public class RouteAdminServiceImpl implements RouteAdminService {

    private final RouteDefinitionJpaRepository jpaRepository;
    private final RedisRouteDefinitionRepository redisRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Flux<RouteDefinitionPayload> getAllRoutes() {
        return Mono.fromCallable(jpaRepository::findAll)
                .flatMapMany(Flux::fromIterable)
                .map(this::convertToRouteDefinitionPayload)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<Void> save(RouteDefinitionPayload payload) {
        log.info("Saving route definition for ID: {}", payload.getId());

        // 1. Save the complete payload (with all filters) to the database.
        RouteDefinitionEntity entity = convertToEntity(payload);
        Mono<Void> saveToDbMono = Mono.fromRunnable(() -> jpaRepository.save(entity))
                .subscribeOn(Schedulers.boundedElastic()).then();

        // 2. Create a standard RouteDefinition for Redis, containing ONLY the enabled filters.
        RouteDefinition redisRouteDefinition = new RouteDefinition();
        redisRouteDefinition.setId(payload.getId());
        redisRouteDefinition.setUri(URI.create(payload.getUri()));
        redisRouteDefinition.setOrder(payload.getOrder());
        redisRouteDefinition.setPredicates(payload.getPredicates());

        List<FilterDefinition> enabledFilters = payload.getFilters().stream()
                .filter(FilterInfo::isEnabled)
                .map(filterInfo -> {
                    FilterDefinition fd = new FilterDefinition();
                    fd.setName(filterInfo.getName());
                    fd.setArgs(filterInfo.getArgs());
                    return fd;
                })
                .collect(Collectors.toList());
        redisRouteDefinition.setFilters(enabledFilters);

        log.info("Publishing route [{}] to Redis with {} active filters.", redisRouteDefinition.getId(), enabledFilters.size());

        // 3. Save the filtered route definition to Redis.
        Mono<Void> saveToRedisMono = redisRepository.save(Mono.just(redisRouteDefinition));

        return saveToDbMono.then(saveToRedisMono);
    }

    @Override
    @Transactional
    public Mono<Void> delete(String routeId) {
        log.info("Deleting route from database and Redis: [{}]", routeId);
        Mono<Void> deleteFromDbMono = Mono.fromRunnable(() -> jpaRepository.deleteById(routeId))
                .subscribeOn(Schedulers.boundedElastic()).then();
        Mono<Void> deleteFromRedisMono = redisRepository.delete(Mono.just(routeId));
        return deleteFromDbMono.then(deleteFromRedisMono);
    }

    @Override
    public void refreshRoutes() {
        log.info("Manually triggering a global route refresh.");
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }

    @SneakyThrows
    private RouteDefinitionEntity convertToEntity(RouteDefinitionPayload payload) {
        var entity = new RouteDefinitionEntity();
        entity.setId(payload.getId());
        entity.setUri(payload.getUri());
        entity.setRouteOrder(payload.getOrder());
        entity.setPredicates(objectMapper.writeValueAsString(payload.getPredicates()));
        // Store the full list of FilterInfo objects as JSON
        entity.setFilters(objectMapper.writeValueAsString(payload.getFilters()));
        return entity;
    }

    @SneakyThrows
    private RouteDefinitionPayload convertToRouteDefinitionPayload(RouteDefinitionEntity entity) {
        var payload = new RouteDefinitionPayload();
        payload.setId(entity.getId());
        payload.setUri(entity.getUri());
        payload.setOrder(entity.getRouteOrder());
        payload.setPredicates(objectMapper.readValue(entity.getPredicates(), new TypeReference<List<PredicateDefinition>>() {}));
        // Read the full list of FilterInfo objects from JSON
        payload.setFilters(objectMapper.readValue(entity.getFilters(), new TypeReference<List<FilterInfo>>() {}));
        return payload;
    }
}
