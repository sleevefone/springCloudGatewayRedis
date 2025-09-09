package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * A DTO to hold the discovered factory information.
 * Using a record for a concise, immutable data carrier.
 */
record FactoriesInfoPayload(List<GatewayFilterService.FactoryInfo> predicates, List<GatewayFilterService.FactoryInfo> filters) {}

/**
 * Admin API to expose lists of available GatewayFilter and RoutePredicate factories with their arguments.
 */
@RestController
@RequestMapping("/__gateway/admin/factories")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.admin", name = "enabled", havingValue = "true")
public class GatewayFilterAdminController {

    private final GatewayFilterService gatewayFilterService;

    /**
     * Returns a payload containing detailed information about all discovered factories, including their arguments.
     * @return A DTO containing both predicate and filter factory details.
     */
    @GetMapping
    public FactoriesInfoPayload getAvailableFactories() {
        return new FactoriesInfoPayload(
            gatewayFilterService.getAvailablePredicates(),
            gatewayFilterService.getAvailableFilters()
        );
    }
}
