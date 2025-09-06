package com.ocft.gateway.openapi.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 一个全局过滤器，用于记录所有进入网关的请求。
 * 它会记录请求方法、路径、查询参数，以及POST/PUT等请求的请求体。
 * 它实现了 GlobalFilter 接口，因此会自动应用于所有路由。
 */
@Slf4j
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        boolean hasBody = method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        // 1. 为所有请求记录基本信息，包括路径和查询参数
        if (!hasBody || !CollectionUtils.isEmpty(queryParams)) {
            log.info("=> {} {} REQ::PARAM: {}", method, request.getURI().getPath(), request.getQueryParams());
        }
        // 2. 如果请求包含请求体，则额外记录请求体
        if (hasBody) {
            return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
                Mono<String> cachedBody = serverHttpRequest.getBody()
                        .map(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            return new String(bytes, StandardCharsets.UTF_8);
                        })
                        .collectList()
                        .map(list -> String.join("", list))
                        .defaultIfEmpty("");

                return cachedBody.flatMap(body -> {
                    if (!body.isEmpty()) {
                        // 使用 DEBUG 级别记录请求体，避免在生产环境中产生过多日志
                        log.info("REQ::BODY: {}", body.replaceAll("[\r\n\t]", ""));
                    }
                    // 将包含了缓存请求体的 request 重新构建到 exchange 中，并继续过滤器链
                    return chain.filter(exchange.mutate().request(serverHttpRequest).build());
                });
            });
        }

        // 3. 对于没有请求体的请求 (GET, DELETE 等)，直接继续过滤器链
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 设置一个较高的优先级（数值小），以确保它在其他业务过滤器之前执行
        // 例如，-1 会让它在大多数默认过滤器之后，但在路由过滤器之前运行
        return -1;
    }
}
