package com.ocft.gateway.openapi.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Slf4j
@RequiredArgsConstructor
public class RedisRouteDefinitionRepository
//        implements RouteDefinitionRepository
{

    private static final String ROUTES_KEY = "gateway:routes";
    public static final String REFRESH_ROUTES_CHANNEL = "gateway:routes:refresh";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public Flux<RouteDefinition> getRouteDefinitions() {
        log.debug("Loading routes from Redis.");
        return redisTemplate.opsForHash().values(ROUTES_KEY)
                .flatMap(routeJson -> {
                    try {
                        return Mono.just(objectMapper.readValue((String) routeJson, RouteDefinition.class));
                    } catch (JsonProcessingException e) {
                        // A more resilient approach: log the error and skip the invalid route
                        // instead of failing the entire stream.
                        log.error("Failed to parse route definition from Redis: {}", routeJson, e);
                        return Mono.empty();
                    }
                })
                .doOnComplete(() -> log.info("Successfully loaded routes from Redis."))
                .doOnError(error -> log.error("Error loading routes from Redis.", error));
    }

    public Mono<Void> save(Mono<RouteDefinition> routeDefinitionMono) {
        return routeDefinitionMono.flatMap(routeDefinition -> {
            try {
                String routeJson = objectMapper.writeValueAsString(routeDefinition);
                log.info("Saving route to Redis: [{}]", routeDefinition.getId());
                return redisTemplate.opsForHash()
                        .put(ROUTES_KEY, routeDefinition.getId(), routeJson)
                        .then(Mono.fromRunnable(this::notifyChanged));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize route definition for saving: [{}]", routeDefinition.getId(), e);
                return Mono.error(e);
            }
        });
    }

    public Mono<Void> delete(Mono<String> routeIdMono) {
        return routeIdMono.flatMap(routeId -> {
            log.info("Deleting route from Redis: [{}]", routeId);
            return redisTemplate.opsForHash()
                    .remove(ROUTES_KEY, routeId)
                    .then(Mono.fromRunnable(this::notifyChanged));
        });
    }

    /**
     * Publishes a RefreshRoutesEvent to notify the gateway to reload routes.
     */
    private void notifyChanged() {
        // 1. Publish to Redis channel for other nodes
        log.info("Publishing route change notification to Redis channel: {}", REFRESH_ROUTES_CHANNEL);
        redisTemplate.convertAndSend(REFRESH_ROUTES_CHANNEL, "refresh")
                .subscribe(
                        null, // onNext is not needed for fire-and-forget
                        error -> log.error("Failed to publish route refresh message to Redis.", error)
                );

        // 2. Publish local event for immediate refresh on this node
        log.info("Publishing local RefreshRoutesEvent.");
        this.eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
