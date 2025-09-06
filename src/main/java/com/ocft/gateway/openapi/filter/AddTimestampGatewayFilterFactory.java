package com.ocft.gateway.openapi.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 一个自定义的网关过滤器工厂。
 * <p>
 * 功能：向请求头中添加一个时间戳。
 * <p>
 * 命名约定：类名 'AddTimestamp' + 'GatewayFilterFactory'。
 * 在配置文件中引用时，使用 'AddTimestamp'。
 */
@Component
@Slf4j
public class AddTimestampGatewayFilterFactory extends AbstractGatewayFilterFactory<AddTimestampGatewayFilterFactory.Config> {

    private static final String DEFAULT_HEADER_NAME = "X-Request-Timestamp";

    public AddTimestampGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 如果配置中未提供 headerName，则使用默认值
            String headerNameToUse = StringUtils.hasText(config.getHeaderName()) ? config.getHeaderName() : DEFAULT_HEADER_NAME;

            log.info("Executing AddTimestamp filter, adding header: '{}'", headerNameToUse);

            // 修改请求，添加一个新的请求头
            var requestWithHeader = exchange.getRequest().mutate()
                    .header(headerNameToUse, String.valueOf(System.currentTimeMillis()))
                    .build();

            var newExchange = exchange.mutate().request(requestWithHeader).build();

            return chain.filter(newExchange);
        };
    }

    /**
     * 配置类，用于接收来自路由定义的参数。
     * 字段名必须与路由定义中 'args' 的键名匹配。
     */
    @Data // Lombok's @Data generates getters, setters, toString, etc.
    public static class Config {
        private String headerName;
    }
}