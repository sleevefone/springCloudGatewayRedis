package com.ocft.gateway.openapi.filter;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * A GatewayFilterFactory for Canary (or A/B) routing.
 * <p>
 * This filter checks for a specific header and value in the request. If a match is found,
 * it dynamically forwards the request to a specified "canary" URI, overriding the
 * original URI defined in the route. If no match is found, the request proceeds to the
 * original "stable" URI.
 * <p>
 * Configuration example in JSON:
 * {
 *   "name": "CanaryRouting",
 *   "args": {
 *     "headerName": "X-Version",
 *     "headerValue": "v2",
 *     "canaryUri": "http://my-canary-service:8080"
 *   }
 * }
 */
@Component
@Slf4j
public class CanaryRoutingGatewayFilterFactory extends AbstractGatewayFilterFactory<CanaryRoutingGatewayFilterFactory.Config> {

    public CanaryRoutingGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String headerValue = exchange.getRequest().getHeaders().getFirst(config.getHeaderName());
            URI originalUri = exchange.getRequest().getURI();

            // Check if the request header matches the canary condition
            if (config.getHeaderValue().equals(headerValue)) {
                log.debug("Canary routing triggered. Header '{}' with value '{}' found.", config.getHeaderName(), headerValue);

                // Build the new URI for the canary service, preserving the original path and query
                URI newUri = UriComponentsBuilder.fromUriString(config.getCanaryUri())
                        .path(originalUri.getRawPath())
                        .query(originalUri.getRawQuery())
                        .build(true)
                        .toUri();

                // Set the new request URL attribute to override the original route's URI
                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, newUri);

                log.info("Canary Route: Request for version '{}' redirected from {} to {}", headerValue, originalUri, newUri);
            }

            // If no match, continue the filter chain to the original 'stable' URI
            return chain.filter(exchange);
        };
    }

    @Validated
    @Data
    public static class Config {
        @NotEmpty
        private String headerName;  // The name of the header to inspect (e.g., "X-Version")
        @NotEmpty
        private String headerValue; // The value that triggers canary routing (e.g., "v2")
        @NotEmpty
        private String canaryUri;   // The URI of the canary service (e.g., "http://my-canary-service:8080")
    }
}