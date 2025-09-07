package com.ocft.gateway.openapi.constant.grok;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * A custom gateway filter factory for handling exceptions and returning a standardized JSON error response.
 * <p>
 * Functionality: Catches exceptions, logs them, and returns a JSON response with error code and message.
 * <p>
 * Naming convention: Class name 'ErrorHandling' + 'GatewayFilterFactory'.
 * In configuration, use 'ErrorHandling'.
 */
@Component
@Slf4j
public class ErrorHandlingGatewayFilterFactory11 extends AbstractGatewayFilterFactory<ErrorHandlingGatewayFilterFactory11.Config> {

    private final ObjectMapper objectMapper;

    public ErrorHandlingGatewayFilterFactory11(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> chain.filter(exchange).onErrorResume(Exception.class, e -> {
            log.error("Error occurred in request: {}", exchange.getRequest().getURI(), e);

            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("errorCode", config.getErrorCode() != null ? config.getErrorCode() : "500");
            errorResponse.put("message", e.getMessage() != null ? e.getMessage() : "Internal Server Error");

            try {
                byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            } catch (Exception ex) {
                log.error("Failed to serialize error response", ex);
                return exchange.getResponse().setComplete();
            }
        });
    }

    /**
     * Configuration class for receiving parameters from route definitions.
     */
    @Data
    public static class Config {
        private String errorCode;
    }
}