package com.ocft.gateway.openapi.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;


/**
 * Listens to Redis Pub/Sub channel for route refresh notifications.
 * This ensures that all gateway instances in a cluster refresh their routes
 * when a change is made on any single instance.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RouteRefreshListener {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @PostConstruct
    public void init() {
        this.redisTemplate
                .listenToChannel(RedisRouteDefinitionRepository.REFRESH_ROUTES_CHANNEL)
                .doOnNext(message -> {
                    log.info("Received route refresh notification from Redis channel: {}", message.getMessage());
                    // Publish a local event to trigger the route refresh
                    eventPublisher.publishEvent(new RefreshRoutesEvent(this));
                })
                .doOnError(error -> log.error("Error listening to Redis route refresh channel.", error))
                .subscribe(); // Must subscribe to start listening
        log.info("Subscribed to Redis route refresh channel: {}", RedisRouteDefinitionRepository.REFRESH_ROUTES_CHANNEL);
    }
}