//package com.ocft.gateway.openapi.filter;
//
//import com.google.common.util.concurrent.RateLimiter;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.concurrent.ConcurrentHashMap;
//
//@Component
//public class RateLimiterFilter implements GlobalFilter {
//
//    // 存储每个键的 RateLimiter，线程安全
//    private final ConcurrentHashMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
//
//    // 配置限流参数：每秒速率（可从配置中心动态加载）
//    private static final double RATE = 10.0; // 每秒10个请求
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        // 获取限流键
//        String key = getRateLimitKey(exchange);
//
//        // 获取或创建 RateLimiter
//        RateLimiter limiter = rateLimiters.computeIfAbsent(key, k -> RateLimiter.create(RATE));
//
//        // 尝试获取令牌
//        if (!limiter.tryAcquire()) {
//            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
//            return exchange.getResponse().setComplete();
//        }
//
//        return chain.filter(exchange);
//    }
//
//    private String getRateLimitKey(ServerWebExchange exchange) {
//        // 优先从 header 获取 AppKey
//        String appKey = exchange.getRequest().getHeaders().getFirst("X-App-Key");
//        if (appKey != null && !appKey.isEmpty()) {
//            return "appkey:" + appKey;
//        }
//        // 次之 IP
//        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
//        if (ip != null) {
//            return "ip:" + ip;
//        }
//        // 最后路径
//        return "path:" + exchange.getRequest().getURI().getPath();
//    }
//}