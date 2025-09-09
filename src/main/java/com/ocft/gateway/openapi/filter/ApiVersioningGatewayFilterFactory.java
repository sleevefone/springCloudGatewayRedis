package com.ocft.gateway.openapi.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * A GatewayFilterFactory for API versioning, supporting both path-based and header-based strategies.
 */
@Component
@Slf4j
public class ApiVersioningGatewayFilterFactory extends AbstractGatewayFilterFactory<ApiVersioningGatewayFilterFactory.Config> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("/v(\\d+)");

    public ApiVersioningGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String version = null;

            // Strategy 1: Path-based versioning
            if (config.isUsePathPrefix()) {
                Matcher matcher = VERSION_PATTERN.matcher(request.getURI().getPath());
                if (matcher.find()) {
                    version = "v" + matcher.group(1);
                    // Remove the version prefix from the path for the backend
                    String newPath = matcher.replaceFirst("");
                    request = request.mutate().path(newPath).build();
                }
            }

            // Strategy 2: Header-based versioning (if path version not found)
            if (version == null && config.getVersionHeaderName() != null) {
                version = request.getHeaders().getFirst(config.getVersionHeaderName());
            }

            // Determine the target backend URI
            String backendUri = config.getVersionBackends().get(version);
            if (backendUri == null) {
                backendUri = config.getDefaultBackend();
            }

            if (backendUri == null) {
                log.warn("No backend found for version '{}' and no default backend is configured.", version);
                return chain.filter(exchange.mutate().request(request).build());
            }

            // Reconstruct the final URI
            URI newUri = UriComponentsBuilder.fromUriString(backendUri)
                    .path(request.getURI().getPath())
                    .query(request.getURI().getQuery())
                    .build(true)
                    .toUri();

            log.debug("API Versioning: Forwarding request for version '{}' to {}", version, newUri);

            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, newUri);
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    @Data
    public static class Config {
        private boolean usePathPrefix = false;
        private String versionHeaderName = "X-Api-Version";
        private Map<String, String> versionBackends = new HashMap<>();
        private String defaultBackend;
    }
}
