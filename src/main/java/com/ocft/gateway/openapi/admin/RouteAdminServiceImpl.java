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
import org.springframework.util.StringUtils;
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
    public Flux<RouteDefinitionPayload> getAllRoutes(String query) {
        // If query is null or blank, return all routes
        if (!StringUtils.hasText(query)) {
            return Mono.fromCallable(jpaRepository::findAll)
                    .flatMapMany(Flux::fromIterable)
                    .map(this::convertToRouteDefinitionPayload)
                    .subscribeOn(Schedulers.boundedElastic());
        }
        // If there is a query, perform a search
        else {
            return Mono.fromCallable(() -> jpaRepository.findByIdContainingIgnoreCaseOrUriContainingIgnoreCase(query, query))
                    .flatMapMany(Flux::fromIterable)
                    .map(this::convertToRouteDefinitionPayload)
                    .subscribeOn(Schedulers.boundedElastic());
        }
    }

    @Override
    @Transactional
    public Mono<Void> save(RouteDefinitionPayload payload) {
        log.info("Saving route [{}], enabled status: {}", payload.getId(), payload.isEnabled());

        // 1. Save the complete payload to the database.
        RouteDefinitionEntity entity = convertToEntity(payload);
        Mono<Void> saveToDbMono = Mono.fromRunnable(() -> jpaRepository.save(entity))
                .subscribeOn(Schedulers.boundedElastic()).then();

        // 2. Based on the 'enabled' flag, either publish the route to Redis or delete it from Redis.
        Mono<Void> redisOperationMono;
        if (payload.isEnabled()) {
            log.info("Route [{}] is enabled. Publishing to Redis.", payload.getId());
            RouteDefinition redisRouteDefinition = new RouteDefinition();
            redisRouteDefinition.setId(payload.getId());
            redisRouteDefinition.setUri(URI.create(payload.getUri()));
            redisRouteDefinition.setOrder(payload.getOrder());
            redisRouteDefinition.setPredicates(payload.getPredicates());
            redisRouteDefinition.setFilters(payload.getFilters().stream()
                    .filter(FilterInfo::isEnabled)
                    .map(this::convertToFilterDefinition)
                    .collect(Collectors.toList()));
            redisOperationMono = redisRepository.save(Mono.just(redisRouteDefinition));
        } else {
            log.info("Route [{}] is disabled. Deleting from Redis.", payload.getId());
            redisOperationMono = redisRepository.delete(Mono.just(payload.getId()));
        }

        return saveToDbMono.then(redisOperationMono);
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
    public Mono<RouteDefinitionPayload> getById(String routeId) {
        log.info("get route from database and Redis: [{}]", routeId);
        return Mono.fromCallable(() -> jpaRepository.findById(routeId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalEntity -> optionalEntity.map(this::convertToRouteDefinitionPayload)
                        .map(Mono::just)
                        .orElseGet(Mono::empty));
    }

    @Override
    public void refreshRoutes() {
        log.info("Manually triggering a global route refresh.");
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }

    // --- Helper Methods ---

    private FilterDefinition convertToFilterDefinition(FilterInfo filterInfo) {
        FilterDefinition fd = new FilterDefinition();
        fd.setName(filterInfo.getName());
        fd.setArgs(filterInfo.getArgs());
        return fd;
    }

    @SneakyThrows
    private RouteDefinitionEntity convertToEntity(RouteDefinitionPayload payload) {
        var entity = new RouteDefinitionEntity();
        entity.setId(payload.getId());
        entity.setUri(payload.getUri());
        entity.setRouteOrder(payload.getOrder());
        entity.setEnabled(payload.isEnabled()); // Save enabled state
        entity.setPredicates(objectMapper.writeValueAsString(payload.getPredicates()));
        entity.setFilters(objectMapper.writeValueAsString(payload.getFilters()));
        return entity;
    }

    @SneakyThrows
    private RouteDefinitionPayload convertToRouteDefinitionPayload(RouteDefinitionEntity entity) {
        var payload = new RouteDefinitionPayload();
        payload.setId(entity.getId());
        payload.setUri(entity.getUri());
        payload.setOrder(entity.getRouteOrder());
        payload.setEnabled(entity.isEnabled()); // Read enabled state
        payload.setPredicates(objectMapper.readValue(entity.getPredicates(), new TypeReference<List<PredicateDefinition>>() {}));
        payload.setFilters(objectMapper.readValue(entity.getFilters(), new TypeReference<List<FilterInfo>>() {}));
        return payload;
    }
}
