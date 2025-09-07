package com.ocft.gateway.openapi.constant.grok;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@Order(-1) // 确保在默认的异常处理器之前执行
@Slf4j
public class NotFoundExceptionHandler implements ErrorWebExceptionHandler {


    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // 检查响应状态码是否为 404
        if (response.getStatusCode() == HttpStatus.NOT_FOUND || ex instanceof NoResourceFoundException) {
            String path = exchange.getRequest().getURI().getPath();
            String method = Objects.requireNonNull(exchange.getRequest().getMethod()).name();
            String traceId = exchange.getRequest().getHeaders().getFirst("traceId");

            log.error("404 Not Found - Path: {}, Method: {}, cause: {},TraceId: {}",
                    path,
                    method,
                    ex.getMessage(),
                    traceId
            );
        } else {
            log.error("unknown error", ex);
        }

        // 传递异常，让默认的异常处理器继续处理，以返回标准的错误响应体
        return Mono.error(ex);
    }
}