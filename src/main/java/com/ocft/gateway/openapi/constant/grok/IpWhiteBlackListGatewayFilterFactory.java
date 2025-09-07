package com.ocft.gateway.openapi.constant.grok;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class IpWhiteBlackListGatewayFilterFactory extends AbstractGatewayFilterFactory<IpWhiteBlackListGatewayFilterFactory.Config> {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public IpWhiteBlackListGatewayFilterFactory(ReactiveRedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // Extract client IP
            String clientIp = getClientIp(request);

            // Check black/white list based on mode
            if ("whitelist".equalsIgnoreCase(config.getMode())) {
                return redisTemplate.opsForSet().isMember(config.getWhiteListKey(), clientIp)
                        .flatMap(isInWhiteList -> {
                            if (Boolean.TRUE.equals(isInWhiteList)) {
                                return chain.filter(exchange); // Allow request
                            } else {
                                return handleUnauthorized(response, "IP not in whitelist");
                            }
                        });
            } else {
                return redisTemplate.opsForSet().isMember(config.getBlackListKey(), clientIp)
                        .flatMap(isInBlackList -> {
                            if (Boolean.FALSE.equals(isInBlackList)) {
                                return chain.filter(exchange); // Allow request
                            } else {
                                return handleUnauthorized(response, "IP in blacklist");
                            }
                        });
            }
        };
    }

    private String getClientIp(ServerHttpRequest request) {
        // Handle X-Forwarded-For for proxies
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // Take the first IP in case of multiple proxies
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }

    private Mono<Void> handleUnauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("Content-Type", "application/json");
        String errorBody = "{\"error\": \"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        private String mode; // "whitelist" or "blacklist"
        private String whiteListKey; // Redis key for whitelist
        private String blackListKey; // Redis key for blacklist

        // Getters and Setters
        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getWhiteListKey() {
            return whiteListKey;
        }

        public void setWhiteListKey(String whiteListKey) {
            this.whiteListKey = whiteListKey;
        }

        public String getBlackListKey() {
            return blackListKey;
        }

        public void setBlackListKey(String blackListKey) {
            this.blackListKey = blackListKey;
        }
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("mode", "whiteListKey", "blackListKey");
    }
}