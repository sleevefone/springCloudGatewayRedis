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
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

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
        // 装饰响应以拦截 headers
        ServerHttpResponse decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public boolean setStatusCode(HttpStatusCode status) {
                boolean result = super.setStatusCode(status);
                if (status != null && status.isError()) {
                    addErrorHeader(exchange, status);
                }
                return result;
            }

            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                HttpStatusCode status = getStatusCode();
                if (status != null && status.isError() && !getHeaders().containsKey(ERROR_HEADER_NAME)) {
                    addErrorHeader(exchange, status);
                }
                return super.writeWith(body);
            }
        };

        // 替换原始响应
        ServerWebExchange mutatedExchange = exchange.mutate().response(decoratedResponse).build();

        return chain.filter(mutatedExchange).onErrorResume(Exception.class, e -> {
            log.error("Error occurred in request: {}", exchange.getRequest().getURI(), e);

            ServerHttpResponse response = mutatedExchange.getResponse();
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);

            // 添加错误 header
            String errorCode = getErrorCode(exchange, HttpStatus.INTERNAL_SERVER_ERROR);
            response.getHeaders().addIfAbsent(ERROR_HEADER_NAME, errorCode);

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
            return response.writeWith(Mono.just(buffer));
        });
    }

    private void addErrorHeader(ServerWebExchange exchange, HttpStatusCode status) {
        String errorCode = getErrorCode(exchange, status);
        exchange.getResponse().getHeaders().addIfAbsent(ERROR_HEADER_NAME, errorCode);
    }

    private String getErrorCode(ServerWebExchange exchange, HttpStatusCode status) {
        Object routeId = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String system = (routeId != null) ? routeId.toString() : "DEFAULT";
        return system + "-" + status.value();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // 提前执行，确保 headers 可写
    }
}