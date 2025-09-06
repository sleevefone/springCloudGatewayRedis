package com.ocft.gateway.openapi.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;

import static com.ocft.gateway.openapi.config.DatabaseRouteDefinitionRepository.REFRESH_ROUTES_CHANNEL;


/**
 * Listens to Redis Pub/Sub channel for route refresh notifications.
 * This ensures that all gateway instances in a cluster refresh their routes
 * when a change is made on any single instance.
 * The subscription includes a retry mechanism to enhance resilience against transient startup errors.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisRouteRefreshListener {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @PostConstruct
    public void init() {
        this.redisTemplate
                .listenToChannel(REFRESH_ROUTES_CHANNEL)
                .doOnNext(message -> {
                    log.info("Received route refresh notification from Redis channel: {}", message.getMessage());
                    // Publish a local event to trigger the route refresh
                    eventPublisher.publishEvent(new RefreshRoutesEvent(this));
                })
                .doOnError(error -> log.error("Error listening to Redis route refresh channel.", error))
                // Add a retry mechanism for resilience during startup or transient network issues.
                // It will retry 3 times with an exponential backoff starting at 2 seconds.
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .subscribe(); // Must subscribe to start listening
        log.info("Subscribed to Redis route refresh channel: {}", REFRESH_ROUTES_CHANNEL);
    }
}