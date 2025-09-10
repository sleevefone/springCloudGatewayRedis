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

/**
 * Provides endpoints to retrieve the source code of gateway components.
 */
@RestController
@RequestMapping("/code")
@RequiredArgsConstructor
public class SourceCodeController {

    private final GatewayFilterService gatewayFilterService;
    private final SourceCodeService sourceCodeService;

    /**
     * Retrieves the source code for a given GatewayFilterFactory short name.
     * @param filterName The short name of the filter factory (e.g., "AddRequestHeader").
     * @return The Java source code as a plain text response, or a 404 if not found.
     */
    @GetMapping(value = "/filter/sourceCode", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getFilterSourceCode(@RequestParam("name") String filterName) {
        // Find the factory info by the short name, ignoring case.
        return gatewayFilterService.getAvailableFilters().stream()
                .filter(factoryInfo -> factoryInfo.name().equalsIgnoreCase(filterName))
                .findFirst()
                .map(factoryInfo -> {
                    try {
                        // Use the full class name to get the source code.
                        String sourceCode = sourceCodeService.getSourceCode(factoryInfo.className());
                        return ResponseEntity.ok(sourceCode);
                    } catch (IOException e) {
                        // Handle potential errors during file reading.
                        return ResponseEntity.status(500).body("Error reading source code: " + e.getMessage());
                    }
                })
                // If no factory with that name is found, return a 404.
                .orElse(ResponseEntity.status(404).body("Filter factory not found: " + filterName));
    }
}
