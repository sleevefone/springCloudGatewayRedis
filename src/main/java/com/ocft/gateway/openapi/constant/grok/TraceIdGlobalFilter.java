//package com.ocft.gateway.openapi.constant.grok;
//
//import io.micrometer.context.ContextSnapshot;
//import org.slf4j.MDC;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.core.Ordered;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//import reactor.util.context.Context;
//
//import java.util.UUID;
//
//@Component
//public class TraceIdGlobalFilter implements GlobalFilter, Ordered {
//
//    private static final String TRACE_ID_HEADER = "traceId";
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // 提取或生成 traceId
//        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
//        if (traceId == null || traceId.isEmpty()) {
//            traceId = UUID.randomUUID().toString().replace("-", "");
//        }
//
//        // 传播到请求头
//        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
//                .header(TRACE_ID_HEADER, traceId)
//                .build();
//
//        // 传播到响应头（可选）
//        ServerHttpResponse response = exchange.getResponse();
//        response.getHeaders().add(TRACE_ID_HEADER, traceId);
//
//        // 将 traceId 写入 Reactor Context
//        String finalTraceId = traceId;
//        return Mono.defer(() -> {
//            // 使用 ContextSnapshot 恢复 MDC（仅在日志点需要）
//            try (ContextSnapshot.Scope scope = ContextSnapshot.setAllThreadLocalsFrom(
//                    Context.of(TraceIdThreadLocalAccessor.TRACE_ID_KEY, finalTraceId))) {
//                return chain.filter(exchange.mutate().request(mutatedRequest).build());
//            }
//        })
//                .contextWrite(Context.of(TraceIdThreadLocalAccessor.TRACE_ID_KEY, finalTraceId))
//                .doFinally(signalType -> {
//                    // 清理 MDC
//                    MDC.remove(TraceIdThreadLocalAccessor.TRACE_ID_KEY);
//                });
//    }
//
//    @Override
//    public int getOrder() {
//        return Ordered.HIGHEST_PRECEDENCE; // 优先级最高
//    }
//}