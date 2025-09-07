package com.ocft.gateway.openapi.constant.grok;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * A global filter for handling exceptions and adding error headers to error responses.
 * <p>
 * Functionality: For error responses (4xx/5xx) from downstream, adds an error code header if not present.
 * For gateway-level exceptions, sets status, adds error code header, and returns a JSON response with message.
 * The error code is based on the route ID (system identifier) and status code.
 * <p>
 * Header: X-Error-Code (e.g., "systemA-500")
 */
@Component
@Slf4j
public class ErrorHandlingGlobalFilter implements GlobalFilter, Ordered {

    private static final String ERROR_HEADER_NAME = "X-Error-Code";

    private final ObjectMapper objectMapper;

    public ErrorHandlingGlobalFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Mono<Object> objectMono = chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpStatusCode status = response.getStatusCode();
            if (status != null && status.isError()) {
                if (!response.getHeaders().containsKey(ERROR_HEADER_NAME)) {
                    String routeId = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    String system = (routeId != null) ? routeId : "DEFAULT";
                    String errorCode = system + "-" + status.value();
                    response.getHeaders().add(ERROR_HEADER_NAME, errorCode);
                }
            }
        })).onErrorResume(Exception.class, e -> {
            log.error("Error occurred in request: {}", exchange.getRequest().getURI(), e);

            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

            String routeId = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String system = (routeId != null) ? routeId : "DEFAULT";
            String errorCode = system + "-500";

            if (!response.getHeaders().containsKey(ERROR_HEADER_NAME)) {
                response.getHeaders().add(ERROR_HEADER_NAME, errorCode);
            }

            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Internal Server Error");

            byte[] bytes;
            try {
                bytes = objectMapper.writeValueAsBytes(errorResponse);
            } catch (Exception ex) {
                log.error("Failed to serialize error response", ex);
                return response.setComplete();
            }

            DataBuffer buffer = response.bufferFactory().wrap(bytes);
//            return response.writeWith(Mono.just(buffer));
            return response.writeWith(Mono.just(buffer));
        });
        return objectMono.then(Mono.empty());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // Run as a post-filter for response modification
    }
}