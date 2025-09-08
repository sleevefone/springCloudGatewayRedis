package com.ocft.gateway.openapi.filter.global;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class CaffeineRateLimiterFilter implements GlobalFilter {

    // 限流参数：每秒10个请求
    private static final double RATE = 1.0;

    // 存储每个键的令牌桶状态
    private final LoadingCache<@NonNull String, TokenBucket> rateLimiters;

    public CaffeineRateLimiterFilter() {
        this.rateLimiters = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1)) // 1小时无访问则清理
                .build(key -> new TokenBucket(RATE));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String key = getRateLimitKey(exchange);
        TokenBucket bucket = rateLimiters.get(key);

        if (!bucket.tryAcquire()) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private String getRateLimitKey(ServerWebExchange exchange) {
        String appKey = exchange.getRequest().getHeaders().getFirst("X-App-Key");
        if (appKey != null && !appKey.isEmpty()) {
            return "appkey:" + appKey;
        }
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null)
            return "no remote address";
        String ip = remoteAddress.getAddress().getHostAddress();
        if (ip != null) {
            return "ip:" + ip;
        }
        return "path:" + exchange.getRequest().getURI().getPath();
    }

    // 简单的令牌桶实现
    static class TokenBucket {
        private final double rate; // 每秒令牌数
        private double tokens; // 当前令牌数
        private long lastRefillTimestamp; // 上次填充时间
        private final double capacity; // 桶容量

        public TokenBucket(double rate) {
            this.rate = rate;
            this.capacity = rate; // 容量等于速率，允许短时突发
            this.tokens = rate;
            this.lastRefillTimestamp = System.nanoTime();
        }

        public synchronized boolean tryAcquire() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillTimestamp) / 1_000_000_000.0;
            double newTokens = elapsedSeconds * rate;
            tokens = Math.min(capacity, tokens + newTokens);
            lastRefillTimestamp = now;
        }
    }
}