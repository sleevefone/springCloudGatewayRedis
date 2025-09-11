package com.ocft.gateway.openapi.admin;

import com.ocft.gateway.openapi.config.RouteInspectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;

/**
 * Admin API for managing Gateway Routes.
 */
@RestController
// **CRITICAL FIX: Use a dedicated, non-conflicting internal path for admin APIs**
@RequestMapping("/__gateway/admin/routes")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "gateway.admin", name = "enabled", havingValue = "true")
public class RouteAdminController {

    private final RouteAdminService routeAdminService;
    private final RouteInspectorService routeInspectorService;

    @GetMapping
    public Flux<RouteDefinitionPayload> getAllRoutes(@RequestParam(value = "query", required = false) String query) {
        return routeAdminService.getAllRoutes(query);
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createOrUpdateRoute(@RequestBody RouteDefinitionPayload payload) {
        return routeAdminService.save(payload)
                .then(Mono.just(ResponseEntity.created(URI.create("/__gateway/admin/routes/" + payload.getId())).build()))
                .onErrorResume(e -> {
                    log.error("Failed to save route: {}", payload.getId(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save route: " + e.getMessage()));
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteRoute(@PathVariable String id) {
        return routeAdminService.delete(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<RouteDefinitionPayload>> getById(@PathVariable String id) {
        return routeAdminService.getById(id)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<Void>> refreshRoutes() {
        routeAdminService.refreshRoutes();
        return Mono.just(ResponseEntity.ok().build());
    }

    // These hello endpoints seem to be for testing and might be the source of the loop.
    // They should also be on a non-conflicting path if they are to be kept.
    @PostMapping("/hello")
    public Mono<ResponseEntity<RouteDefinitionPayload>> hello() {
        return routeAdminService.getById("dynamic-unit-routing-rule")
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/hello")
    public Mono<ResponseEntity<RouteDefinitionPayload>> getHello(HelloRequestPayload payload) {
        return routeAdminService.getById(Optional.ofNullable(payload.getId()).orElse("dynamic-unit-routing-rule"))
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}
