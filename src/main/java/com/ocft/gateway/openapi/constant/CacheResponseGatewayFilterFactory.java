package com.ocft.gateway.openapi.constant;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * <pre>
 *     curl -X POST http://localhost:8080/actuator/gateway/routes \
 * -H "Content-Type: application/json" \
 * -d '{
 *   "id": "caching-route",
 *   "uri": "http://your-backend-service:8081",
 *   "predicates": [
 *     {
 *       "name": "Path",
 *       "args": {
 *         "patterns": "/products/**"
 *       }
 *     }
 *   ],
 *   "filters": [
 *     {
 *       "name": "Caching",
 *       "args": {
 *         "ttlMinutes": "10"
 *       }
 *     }
 *   ]
 * }'
 * </pre>
 */
@Component
@Slf4j
public class CacheResponseGatewayFilterFactory extends AbstractGatewayFilterFactory<CacheResponseGatewayFilterFactory.Config> {


    // 使用 Caffeine 作为缓存，这里是一个通用的缓存实例，可以根据需要优化为更精细的
    private final Cache<String, String> cache;

    public CacheResponseGatewayFilterFactory() {
        super(Config.class);
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10)) // 默认缓存10分钟
                .maximumSize(10_000) // 最大缓存条目数
                .build();
    }

    @Override
    public GatewayFilter apply(Config config) {

        // 配置中的 ttlMinutes 参数不会被使用，因为Caffeine的缓存策略在创建后不能改变。
        // 如果需要动态TTL，可以考虑为每个请求构建一个具有不同过期策略的新缓存，但会带来性能开销。

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. 只处理 GET 请求
            if (request.getMethod() != HttpMethod.GET) {
                return chain.filter(exchange);
            }

            // 使用 URI 作为缓存 Key
            String cacheKey = request.getURI().toString();

            // 2. 尝试从 Caffeine 缓存中获取响应
            String cachedResponse = cache.getIfPresent(cacheKey);

            if (cachedResponse != null) {
                // 缓存命中：直接返回缓存内容
                log.info("Cache hit for URI: {}", cacheKey);
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.OK);
                // 假设内容类型为 JSON
                response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
                DataBuffer buffer = response.bufferFactory().wrap(cachedResponse.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Flux.just(buffer));
            }

            // 3. 缓存未命中：继续执行过滤器链，并在响应返回前缓存结果
            log.info("Cache miss for URI: {}", cacheKey);

            // 使用装饰器来拦截响应体
            ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
                @NonNull
                public Mono<Void> writeWith(@NonNull org.reactivestreams.Publisher<? extends DataBuffer> body) {
                    return DataBufferUtils.join(Flux.from(body))
                            .flatMap(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                String responseBody = new String(bytes, StandardCharsets.UTF_8);

                                // 将响应体存入 Caffeine
                                cache.put(cacheKey, responseBody);

                                // 将数据重新写回到响应流
                                return super.writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                            });
                }
            };

            // 继续执行过滤器链，但使用装饰器
            return chain.filter(exchange.mutate().response(responseDecorator).build());
        };
    }

    // Config 类，用于从配置文件中接收参数
    public static class Config {
        private int ttlMinutes;

        public int getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
    }
}
