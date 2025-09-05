package com.ocft.gateway.openapi.filter;

import com.ocft.gateway.openapi.config.RedisRouteDefinitionRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 一个自定义的网关过滤器工厂。
 * <p>
 * 功能：向请求头中添加一个时间戳。
 * <p>
 * 命名约定：类名 'AddTimestamp' + 'GatewayFilterFactory'。
 * 在配置文件中引用时，使用 'AddTimestamp'。
 */
@Component
public class AddTimestampGatewayFilterFactory extends AbstractGatewayFilterFactory<AddTimestampGatewayFilterFactory.Config> {
    private static final Logger log = LoggerFactory.getLogger(AddTimestampGatewayFilterFactory.class);


    public AddTimestampGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        // apply 方法返回一个 GatewayFilter 实例
        return (exchange, chain) -> {
            log.info("Executing custom filter: AddTimestamp");

            // 修改请求，添加一个新的请求头
            var requestWithHeader = exchange.getRequest().mutate()
                    .header("X-Request-Timestamp", String.valueOf(System.currentTimeMillis()))
                    .build();

            // 使用修改后的请求创建一个新的 ServerWebExchange
            var newExchange = exchange.mutate().request(requestWithHeader).build();

            // 将新的 exchange 传递给过滤器链的下一个环节
            return chain.filter(newExchange);
        };
    }

    // 一个空的配置类，因为我们的过滤器不需要任何参数。
    // 如果需要参数，可以在这里定义字段。
    public static class Config {
    }
}