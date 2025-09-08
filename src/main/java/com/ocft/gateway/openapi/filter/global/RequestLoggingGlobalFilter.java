package com.ocft.gateway.openapi.filter.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 一个全局过滤器，用于记录所有进入网关的请求。
 * 记录请求方法、路径、查询参数、请求头、真实 IP 以及 POST/PUT/PATCH 请求的请求体。
 */
@Slf4j
@Component
public class RequestLoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_REAL_IP = "X-Real-IP";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        boolean hasBody = method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        String realIp = getRealIp(request);

        // 1. 记录基本信息：方法、路径、查询参数、真实 IP
        StringBuilder logMessage = new StringBuilder()
                .append("=> ")
                .append(method)
                .append(" ")
                .append(request.getURI().getPath())
                .append(" REQ::IP: ")
                .append(realIp)
                .append(" REQ::PARAM: ")
                .append(queryParams.isEmpty() ? "{}" : queryParams);

        // 2. 记录请求头（使用 DEBUG 级别避免生产环境日志过多）
        log.info("REQ::HEADERS: {}", request.getHeaders());

        // 3. 记录基本信息（如果没有请求体或有查询参数）
        if (!hasBody || !CollectionUtils.isEmpty(queryParams)) {
            log.info(logMessage.toString());
        }

        // 4. 如果请求包含请求体，则额外记录请求体
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
                    // 使用 DEBUG 级别记录请求体
                    log.info("REQ::BODY: {}", body.replaceAll("[\r\n\t]", ""));
                    // 1. 记录响应头

                    // 如果有请求体，追加到日志
                    if (!CollectionUtils.isEmpty(queryParams)) {
                        log.info(logMessage.toString());
                    }
                    // 继续过滤器链
                    return chain.filter(exchange.mutate().request(serverHttpRequest).build());
                });
            });
        }

        // 5. 对于没有请求体的请求 (GET, DELETE 等)，直接继续过滤器链
        return chain.filter(exchange);
    }

    /**
     * 获取客户端真实 IP。
     * 优先检查 X-Forwarded-For 头，再检查 X-Real-IP，最后使用 remoteAddress。
     */
    private String getRealIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        // 1. 检查 X-Forwarded-For
        List<String> forwardedFor = headers.get(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // 取第一个 IP（客户端真实 IP）
            String ip = forwardedFor.get(0).split(",")[0].trim();
            if (!ip.isEmpty()) {
                return ip;
            }
        }

        // 2. 检查 X-Real-IP
        String realIp = headers.getFirst(X_REAL_IP);
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        // 3. 兜底：使用 remoteAddress
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return -1; // 高优先级，确保在业务过滤器之前执行
    }
}