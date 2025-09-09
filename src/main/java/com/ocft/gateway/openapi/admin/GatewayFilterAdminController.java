package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * A DTO to hold the discovered factory information.
 * Using a record for a concise, immutable data carrier.
 */
record FactoriesInfoPayload(Map<String, String> predicates, Map<String, String> filters) {}

/**
 * Admin API to expose lists of available GatewayFilter and RoutePredicate factories.
 */
@RestController
@RequestMapping("/__gateway/admin/factories") // Using a more general and internal-specific path
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.admin", name = "enabled", havingValue = "true")
public class GatewayFilterAdminController {

    private final GatewayFilterService gatewayFilterService;

    /**
     * Returns a payload containing maps of all discovered factory names to their class names.
     * @return A DTO containing both predicate and filter factories.
     */
    @GetMapping
    public FactoriesInfoPayload getAvailableFactories() {
        return new FactoriesInfoPayload(
            gatewayFilterService.getAvailablePredicates(),
            gatewayFilterService.getAvailableFilters()
        );
    }
}
