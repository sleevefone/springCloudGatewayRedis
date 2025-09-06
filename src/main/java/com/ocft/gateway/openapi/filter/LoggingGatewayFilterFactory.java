package com.ocft.gateway.openapi.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 一个全局过滤器，用于记录所有包含请求体（如POST, PUT）的请求内容。
 * 这对于调试和监控传入的数据非常有用。
 * 它实现了 GlobalFilter 接口，因此会自动应用于所有路由。
 */
@Slf4j
//@Component
public class LoggingGatewayFilterFactory implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 只记录 POST, PUT, PATCH 等可能包含请求体的方法
        HttpMethod method = exchange.getRequest().getMethod();
        if (method != HttpMethod.POST && method != HttpMethod.PUT && method != HttpMethod.PATCH) {
            return chain.filter(exchange);
        }

        // ServerWebExchangeUtils.cacheRequestBody() 会将请求体缓存起来，以便后续可以重复读取
        // 这是因为请求体（Request Body）在响应式流中默认只能被消费一次
        return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
            Mono<String> cachedBody = serverHttpRequest.getBody()
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return new String(bytes, StandardCharsets.UTF_8);
                    })
                    .collectList() // 1. 将所有 body 块收集到一个 List 中
                    .map(list -> String.join("", list)) // 2. 将 List<String> 合并成一个 String
                    .defaultIfEmpty(""); // 3. 如果 body 为空，则提供一个默认的空字符串

            return cachedBody.flatMap(body -> {
                log.info("[Request Body]: {}", body);
                // 将包含了缓存请求体的 request 重新构建到 exchange 中，并继续过滤器链
                return chain.filter(exchange.mutate().request(serverHttpRequest).build());
            });
        });
    }

    @Override
    public int getOrder() {
        // 设置一个较高的优先级（数值小），以确保它在其他业务过滤器之前执行
        // 例如，-1 会让它在大多数默认过滤器之后，但在路由过滤器之前运行
        return -1;
    }
}
