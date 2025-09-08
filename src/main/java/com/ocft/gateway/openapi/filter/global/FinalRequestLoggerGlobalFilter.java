package com.ocft.gateway.openapi.filter.global;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

        // 使用 StringBuilder 来安全地捕获响应体
        StringBuilder responseBodyCapture = new StringBuilder();
        ServerHttpResponseDecorator decoratedResponse = getServerHttpResponseDecorator(exchange, responseBodyCapture);

        // 使用装饰后的 response 创建一个新的 exchange，并继续过滤器链
        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                // 使用 doFinally 确保无论成功、失败还是取消，日志逻辑都会执行
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;

                    // 从 exchange 的属性中获取最终被路由的 URI
                    // 这个属性是由路由过滤器设置的
                    URI routedUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                    URI originalUri = exchange.getRequest().getURI();
                    ServerHttpResponse response = exchange.getResponse();
                    HttpStatusCode statusCode = response.getStatusCode();
                    String responseBody = responseBodyCapture.toString();

                    // 记录摘要日志 (INFO级别)
                    log.info(
                            "Original URI: [{}], Routed To: [{}], Status: [{}], Duration: [{}ms]",
                            originalUri,
                            (routedUri != null) ? routedUri : "N/A (No Route Found)",
                            (statusCode != null) ? statusCode.value() : "N/A",
                            duration
                    );
                    log.info("RESP::HEADERS: {}, realIp: {},STATUS: {}", response.getHeaders(),
                            FilterUtil.getRealIp(exchange.getRequest()), response.getStatusCode());

                    // 使用 DEBUG 级别记录详细的响应体，避免在生产环境中因日志过多影响性能
                    String respBody = responseBody.replaceAll("[\r\n\t]", "");
                    if (!StringUtils.hasLength(respBody)){
                        respBody = "NULL";
                    }
                    log.info("RESP:BODY: {}", respBody);
                });
    }

    private static ServerHttpResponseDecorator getServerHttpResponseDecorator(ServerWebExchange exchange, StringBuilder responseBodyCapture) {
        ServerHttpResponse originalResponse = exchange.getResponse();

        // 装饰原始的 response，以便我们可以"窥探"响应体
        // 将响应体（body）包装成一个 Flux，以便我们可以使用响应式操作符
        // 在每个数据块（DataBuffer）流过时，将其内容解码并附加到 StringBuilder
        // asByteBuffer() 方法可以安全地读取内容而不消耗它，因此下游消费者仍然可以读取
        return new ServerHttpResponseDecorator(originalResponse) {

            @NonNull
            public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                // 将响应体（body）包装成一个 Flux，以便我们可以使用响应式操作符
                Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                return super.writeWith(fluxBody.doOnNext(dataBuffer -> {
                    // 在每个数据块（DataBuffer）流过时，将其内容解码并附加到 StringBuilder
                    // 使用 asByteBuffer() 可以安全地读取内容而不消耗它，因此下游消费者仍然可以读取
                    int len = dataBuffer.readableByteCount();
                    ByteBuffer result = ByteBuffer.allocate(len);
                    dataBuffer.toByteBuffer(result);
                    responseBodyCapture.append(StandardCharsets.UTF_8.decode(result));
                }));
            }
        };
    }

    @Override
    public int getOrder() {
        // 设置一个非常高的优先级（数值小），以确保它能包裹整个请求生命周期，从而准确计算耗时
        return Ordered.HIGHEST_PRECEDENCE;
    }
}