package com.ocft.gateway.openapi.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 一个全局过滤器，用于记录通过网关的每一个请求的最终转发目的地。
 * 这为调试和监控路由行为提供了至关重要的可观测性。
 */
@Slf4j
@Component
public class FinalRequestLoggerGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        return chain.filter(exchange)
                // 使用 doFinally 确保无论成功、失败还是取消，日志逻辑都会执行
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;

                    // 从 exchange 的属性中获取最终被路由的 URI
                    // 这个属性是由路由过滤器设置的
                    URI routedUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                    URI originalUri = exchange.getRequest().getURI();
                    HttpStatusCode statusCode = exchange.getResponse().getStatusCode();

                    // 记录日志
                    log.info(
                            "[Request Completed] Original: [{}], Routed To: [{}], Status: [{}], Duration: [{}ms]",
                            originalUri,
                            (routedUri != null) ? routedUri : "N/A (No Route Found)",
                            (statusCode != null) ? statusCode.value() : "N/A",
                            duration
                    );
                });
    }

    @Override
    public int getOrder() {
        // 设置一个非常高的优先级（数值小），以确保它能包裹整个请求生命周期，从而准确计算耗时
        return Ordered.HIGHEST_PRECEDENCE;
    }
}