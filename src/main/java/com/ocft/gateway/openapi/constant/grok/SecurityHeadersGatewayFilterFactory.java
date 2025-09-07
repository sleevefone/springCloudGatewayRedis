package com.ocft.gateway.openapi.constant.grok;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecurityHeadersGatewayFilterFactory extends AbstractGatewayFilterFactory<SecurityHeadersGatewayFilterFactory.Config> {
    private static final Logger log = LoggerFactory.getLogger(SecurityHeadersGatewayFilterFactory.class);

    public SecurityHeadersGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            // 添加 Content-Security-Policy 头
            String csp = buildCspHeader(config.allowedDomains);
            headers.add("Content-Security-Policy", csp);
            log.info("Added CSP header: {}", csp);

            // 添加其他安全头
            headers.add("X-Content-Type-Options", "nosniff");
            headers.add("X-XSS-Protection", "1; mode=block");
            headers.add("X-Frame-Options", "DENY");
            headers.add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

            // 继续过滤器链
            return chain.filter(exchange);
        };
    }

    private String buildCspHeader(List<String> allowedDomains) {
        // 默认 CSP 策略
        StringBuilder csp = new StringBuilder("default-src 'self'");
        
        // 如果配置了允许的域名，添加到 CSP
        if (allowedDomains != null && !allowedDomains.isEmpty()) {
            csp.append("; script-src 'self' ");
            csp.append(String.join(" ", allowedDomains));
            csp.append("; style-src 'self' ");
            csp.append(String.join(" ", allowedDomains));
            csp.append("; img-src 'self' ");
            csp.append(String.join(" ", allowedDomains));
            csp.append("; connect-src 'self' ");
            csp.append(String.join(" ", allowedDomains));
        } else {
            csp.append("; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'");
        }
        
        return csp.toString();
    }

    public static class Config {
        private List<String> allowedDomains;

        public List<String> getAllowedDomains() {
            return allowedDomains;
        }

        public void setAllowedDomains(List<String> allowedDomains) {
            this.allowedDomains = allowedDomains;
        }
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("allowedDomains");
    }
}
