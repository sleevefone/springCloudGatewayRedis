package com.ocft.gateway.openapi.constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.ocft.gateway.openapi.constant.TraceKeys.*;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceIdGlobalFilter.class);

    @Override
    public int getOrder() {
        // 越小越靠前，确保最早就注入 traceId
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();

        // 1) 获取或生成 traceId
        String traceId = getString(req);

        // 2) 把 traceId 写回请求头，确保下游也能拿到；同时给响应头也加上，便于排错
        ServerHttpRequest mutatedReq = req.mutate()
                .headers(h -> h.set(TRACE_ID_HEADER, traceId))
                .build();

        ServerHttpResponse resp = exchange.getResponse();
        resp.getHeaders().set(TRACE_ID_HEADER, traceId);

        // 3) 保存到 exchange attribute（便于别的地方随手取）
        ServerWebExchange mutatedEx = exchange.mutate()
                .request(mutatedReq)
                .build();
        mutatedEx.getAttributes().put(TRACE_ID_ATTR, traceId);

        // 4) 进入下游 filter 链前，塞进 Reactor Context，并“抬升”到 MDC
        return chain.filter(mutatedEx)
                .contextWrite(ctx -> ctx.put(TRACE_ID_CTX_KEY, traceId))
                .transform(ReactiveMdcOperator.lift())
                // 5) 你自己的入口/出口日志示例（会带上 %X{traceId}）
                .doFirst(() -> log.info("REQ {} {} from={} ua={}",
                        req.getMethod(), req.getURI(),
                        req.getRemoteAddress(), req.getHeaders().getFirst("User-Agent")))
                .doFinally(sig -> log.info("RES {} {} status={} endSignal={}",
                        req.getMethod(), req.getURI().getRawPath(),
                        resp.getStatusCode(), sig));
    }

    private String getString(ServerHttpRequest req) {
        String traceId = req.getHeaders().getFirst(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = genTraceId();
        }
        return traceId;
    }

    private String genTraceId() {
        // 简洁好看，去掉横杠
        return UUID.randomUUID().toString().replace("-", "");
    }
}
