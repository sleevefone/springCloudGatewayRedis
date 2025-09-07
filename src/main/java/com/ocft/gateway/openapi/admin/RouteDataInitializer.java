package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * A component that runs on application startup to ensure data consistency.
 * It fetches all routes from the database, normalizes them, and saves them back.
 * This process cleans up any legacy data with incorrect casing for predicate/filter names.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RouteDataInitializer implements CommandLineRunner {

    private final RouteAdminService routeAdminService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting route data initialization and normalization...");

        routeAdminService.getAllRoutes(null) // Get all routes
            .flatMap(routeAdminService::save) // Re-save each route, which triggers normalization and Redis update
            .doOnComplete(() -> {
                log.info("Route data initialization and normalization completed successfully.");
                // After all routes are normalized and saved, trigger a final refresh.
                routeAdminService.refreshRoutes();
            })
            .doOnError(error -> log.error("Error during route data initialization!", error))
            .subscribe();
    }
}
