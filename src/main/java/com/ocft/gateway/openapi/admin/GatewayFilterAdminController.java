package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin API to expose the list of available Gateway Filters in the system.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.admin", name = "enabled", havingValue = "true")
@SuppressWarnings("unused")
public class GatewayFilterAdminController {

    private final GatewayFilterService gatewayFilterService;

    /**
     * Returns a list of all discovered GatewayFilterFactory names.
     * The names are derived from the bean names, e.g., "AddTimestampGatewayFilterFactory" becomes "AddTimestamp".
     * @return A list of available filter names.
     */
    @GetMapping("/filters")
    public List<String> getAvailableFilters() {
        return gatewayFilterService.getAvailableFilters();
    }

    @GetMapping("/filter/sourceCode")
    public List<String> getAvailableFilters(@RequestParam("name") String filterName) {
       return null;
    }
}
