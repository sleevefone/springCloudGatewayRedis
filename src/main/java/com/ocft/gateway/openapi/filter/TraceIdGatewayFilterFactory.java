package com.ocft.gateway.openapi.filter;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A GatewayFilterFactory that ensures every request has a unique Trace ID.
 * This is a best-practice for observability and distributed tracing.
 * The @Order annotation ensures this filter runs before others like the signature filter.
 */
@Component
@Order(-1) // Setting a high precedence to ensure it runs early in the filter chain.
public class TraceIdGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    public TraceIdGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            // Try to get the trace ID from the incoming request header
            String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);

            // If the header is not present, generate a new trace ID
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }

            // Put the trace ID into the logging context (MDC)
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // Add the trace ID to the request headers for downstream services
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header(TRACE_ID_HEADER, traceId)
                    .build();

            // Ensure the MDC is cleared after the request is processed to prevent memory leaks
            return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .doFinally(signalType -> MDC.remove(TRACE_ID_MDC_KEY));
        };
    }
}
