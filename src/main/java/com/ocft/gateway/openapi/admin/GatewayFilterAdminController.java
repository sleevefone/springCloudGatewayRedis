package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * A DTO to hold the discovered factory information.
 */
record FactoriesInfoPayload(List<GatewayFilterService.FactoryInfo> predicates, List<GatewayFilterService.FactoryInfo> filters) {}

/**
 * Admin API to expose lists of available factories and their source code.
 */
@RestController
@RequestMapping("/__gateway/admin/factories")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.admin", name = "enabled", havingValue = "true")
public class GatewayFilterAdminController {

    private final GatewayFilterService gatewayFilterService;

    @GetMapping
    public FactoriesInfoPayload getAvailableFactories() {
        return new FactoriesInfoPayload(
            gatewayFilterService.getAvailablePredicates(),
            gatewayFilterService.getAvailableFilters()
        );
    }

    /**
     * Retrieves the source code for a given factory class name.
     * @param className The fully qualified name of the class to look up.
     * @return The source code as plain text, or a 404 response if not found.
     */
    @GetMapping(value = "/source", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getSourceCode(@RequestParam("className") String className) {
        try {
            String sourceCode = gatewayFilterService.getSourceCode(className);
            if (sourceCode.startsWith("Source code not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(sourceCode);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error reading source file: " + e.getMessage());
        }
    }
}
