package com.ocft.gateway.openapi.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A component that runs on application startup to ensure data consistency.
 * It fetches all routes from the database, normalizes them, performs a bulk delete and save to Redis,
 * and triggers a single refresh event to minimize overhead.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RouteDataInitializer implements CommandLineRunner {
    private static final String ROUTES_KEY = "gateway:routes";
    public static final String REFRESH_ROUTES_CHANNEL = "gateway:routes:refresh";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RouteAdminService routeAdminService;
    private final RouteDefinitionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final GatewayFilterService gatewayFilterService;

    @Override
    public void run(String... args) {
        log.info("Starting route data initialization and normalization...");

        routeAdminService.getAllRoutes(null)
                .collectList()
                .flatMap(this::processRoutes)
                .doOnSuccess(v -> {
                    log.info("Route data initialization and normalization completed successfully.");
                    notifyChanged();
                })
                .doOnError(error -> log.error("Error during route data initialization!", error))
                .subscribe();
    }

    private Mono<Void> processRoutes(List<RouteDefinitionPayload> payloads) {
        // Normalize and validate all routes
        List<RouteDefinition> enabledRoutes = payloads.stream()
                .map(payload -> {
                    normalizeDefinitions(payload);
                    RouteDefinitionEntity entity = convertToEntity(payload);
                    checkFilters(payload, entity);
                    checkPredicates(payload, entity);
                    return new RouteDefinitionWithPayload(payload, entity);
                })
                .filter(r -> r.payload.isEnabled())
                .map(r -> convertToRedisRouteDefinition(r.payload))
                .collect(Collectors.toList());

        // Perform bulk delete of existing routes
        return redisTemplate.opsForHash()
                .delete(ROUTES_KEY)
                .then(Mono.defer(() -> saveAll(enabledRoutes)));
    }

    private Mono<Void> saveAll(List<RouteDefinition> routeDefinitions) {
        if (routeDefinitions.isEmpty()) {
            log.info("No enabled routes to save to Redis.");
            return Mono.empty();
        }

        Map<String, String> routeMap = new HashMap<>();
        for (RouteDefinition route : routeDefinitions) {
            try {
                String routeJson = objectMapper.writeValueAsString(route);
                routeMap.put(route.getId(), routeJson);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize route definition for saving: [{}]", route.getId(), e);
                return Mono.error(e);
            }
        }

        log.info("Saving {} routes to Redis in bulk.", routeMap.size());
        return redisTemplate.opsForHash()
                .putAll(ROUTES_KEY, routeMap)
                .then();
    }

    // --- Helper Methods ---

    private void normalizeDefinitions(RouteDefinitionPayload payload) {
        if (payload.getPredicates() != null) {
            payload.getPredicates().forEach(p -> p.setName(StringUtils.capitalize(p.getName())));
        }
        if (payload.getFilters() != null) {
            payload.getFilters().forEach(f -> f.setName(StringUtils.capitalize(f.getName())));
        }
    }

    private RouteDefinition convertToRedisRouteDefinition(RouteDefinitionPayload payload) {
        RouteDefinition redisRouteDefinition = new RouteDefinition();
        redisRouteDefinition.setId(payload.getId());
        redisRouteDefinition.setUri(URI.create(payload.getUri()));
        redisRouteDefinition.setOrder(payload.getOrder());
        redisRouteDefinition.setPredicates(payload.getPredicates());
        redisRouteDefinition.setFilters(payload.getFilters().stream()
                .map(this::convertToFilterDefinition)
                .collect(Collectors.toList()));
        return redisRouteDefinition;
    }

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
        entity.setEnabled(payload.isEnabled());
        entity.setPredicates(objectMapper.writeValueAsString(payload.getPredicates()));
        entity.setFilters(objectMapper.writeValueAsString(payload.getFilters()));
        entity.setPredicateDescription(payload.getPredicateDescription());
        entity.setFilterDescription(payload.getFilterDescription());
        entity.setCreator("admin");
        entity.setUpdater("admin");
        return entity;
    }

    @SneakyThrows
    private RouteDefinitionPayload convertToRouteDefinitionPayload(RouteDefinitionEntity entity) {
        var payload = new RouteDefinitionPayload();
        payload.setId(entity.getId());
        payload.setUri(entity.getUri());
        payload.setOrder(entity.getRouteOrder());
        payload.setEnabled(entity.isEnabled());
        payload.setPredicates(objectMapper.readValue(entity.getPredicates(), new TypeReference<>() {
        }));
        payload.setFilters(objectMapper.readValue(entity.getFilters(), new TypeReference<>() {
        }));
        payload.setPredicateDescription(entity.getPredicateDescription());
        payload.setFilterDescription(entity.getFilterDescription());
        normalizeDefinitions(payload);
        return payload;
    }

    private void checkPredicates(RouteDefinitionPayload payload, RouteDefinitionEntity entity) {
        List<String> predicates = gatewayFilterService.getAvailablePredicates();
        List<String> filter2 = payload.getPredicates().stream().map(PredicateDefinition::getName).collect(Collectors.toList());
        filter2.removeAll(predicates);
        if (!CollectionUtils.isEmpty(filter2)) {
            payload.setEnabled(false);
            entity.setPredicateDescription("predicate(s) not found");
        } else {
            entity.setPredicateDescription(null);
        }
    }

    private void checkFilters(RouteDefinitionPayload payload, RouteDefinitionEntity entity) {
        List<String> filters = gatewayFilterService.getAvailableFilters();
        List<String> filter1 = payload.getFilters().stream().map(FilterInfo::getName).collect(Collectors.toList());
        filter1.removeAll(filters);
        if (!CollectionUtils.isEmpty(filter1)) {
            payload.setEnabled(false);
            entity.setFilterDescription("filter(s) not found");
        } else {
            entity.setFilterDescription(null);
        }
    }

    /**
     * Publishes a RefreshRoutesEvent to notify the gateway to reload routes.
     */
    private void notifyChanged() {
        log.info("Publishing route change notification to Redis channel: {}", REFRESH_ROUTES_CHANNEL);
        redisTemplate.convertAndSend(REFRESH_ROUTES_CHANNEL, "refresh")
                .subscribe(
                        null,
                        error -> log.error("Failed to publish route refresh message to Redis.", error)
                );

        log.info("Publishing local RefreshRoutesEvent.");
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }

    /**
     * Helper class to hold payload and entity together during processing.
     */
    private static class RouteDefinitionWithPayload {
        RouteDefinitionPayload payload;
        RouteDefinitionEntity entity;

        RouteDefinitionWithPayload(RouteDefinitionPayload payload, RouteDefinitionEntity entity) {
            this.payload = payload;
            this.entity = entity;
        }
    }
}