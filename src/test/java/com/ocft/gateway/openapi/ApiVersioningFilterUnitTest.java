package com.ocft.gateway.openapi;

import com.ocft.gateway.openapi.filter.ApiVersioningGatewayFilterFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 单元测试类，用于测试 ApiVersioningGatewayFilterFactory 的行为。
 * 使用 @WebFluxTest 仅加载 Spring WebFlux 相关的 Bean，以实现轻量级测试。
 */
@WebFluxTest(controllers = ApiVersioningGatewayFilterFactory.class)
class ApiVersioningFilterUnitTest {

    @Autowired
    private ApiVersioningGatewayFilterFactory filterFactory;

    /**
     * 测试路径前缀版本控制功能。
     * 验证带有 "/v1/" 前缀的请求路径是否被正确重写到对应的后端服务。
     */
    @Test
    void testPathPrefixVersioning() {
        // 1. 配置过滤器：启用路径前缀模式，并将 "v1" 路由到 "http://v1-service/users"
        ApiVersioningGatewayFilterFactory.Config config = new ApiVersioningGatewayFilterFactory.Config();
        config.setUsePathPrefix(true);
        config.setVersionBackends(Map.of("v1", "http://v1-service/users"));
        GatewayFilter filter = filterFactory.apply(config);

        // 2. 模拟请求：创建一个带有 "/v1/" 前缀的请求，例如 "/api/v1/profile"
        MockServerHttpRequest mockRequest = MockServerHttpRequest.get("http://localhost/api/v1/profile")
                .build();
        MockServerWebExchange mockExchange = MockServerWebExchange.from(mockRequest);

        // 3. 模拟 GatewayFilterChain：用于捕获经过过滤器后的 Exchange 对象
        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);
        when(mockChain.filter(any())).thenReturn(Mono.empty());

        // 4. 应用过滤器并订阅，触发其逻辑
        filter.filter(mockExchange, mockChain).block();

        // 5. 验证结果：
        // a. 验证 filterChain.filter() 方法被调用了一次
        verify(mockChain).filter(any());

        // b. 获取被修改后的请求 URI
        URI rewrittenUri = mockExchange.getRequest().getURI();

        // c. 验证 URI 是否被正确重写
        // 期望的 URI 应该是 "http://v1-service/users/profile"
        String expectedUri = "http://v1-service/users/profile";
        assertThat(rewrittenUri.toString()).isEqualTo(expectedUri);
    }

    /**
     * 测试请求头版本控制功能。
     * 验证带有 "X-API-Version: v2" 请求头的请求是否被正确重写到对应的后端服务。
     */
    @Test
    void testHeaderVersioning() {
        // 1. 配置过滤器：不使用路径前缀，使用请求头 "X-API-Version" 进行版本控制
        ApiVersioningGatewayFilterFactory.Config config = new ApiVersioningGatewayFilterFactory.Config();
        config.setUsePathPrefix(false);
        config.setVersionHeaderName("X-API-Version");
        config.setVersionBackends(Map.of("v2", "http://v2-service/products"));
        GatewayFilter filter = filterFactory.apply(config);

        // 2. 模拟请求：创建一个带有 "X-API-Version: v2" 请求头的请求
        MockServerHttpRequest mockRequest = MockServerHttpRequest.get("http://localhost/api/products/123")
                .header("X-API-Version", "v2")
                .build();
        MockServerWebExchange mockExchange = MockServerWebExchange.from(mockRequest);

        // 3. 模拟 GatewayFilterChain
        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);
        when(mockChain.filter(any())).thenReturn(Mono.empty());

        // 4. 应用过滤器并订阅
        filter.filter(mockExchange, mockChain).block();

        // 5. 验证结果
        verify(mockChain).filter(any());
        URI rewrittenUri = mockExchange.getRequest().getURI();

        // 期望的 URI 应该是 "http://v2-service/products/api/products/123"
        String expectedUri = "http://v2-service/products/api/products/123";
        assertThat(rewrittenUri.toString()).isEqualTo(expectedUri);
    }

    /**
     * 测试无版本信息时的回退功能。
     * 验证当请求既没有版本路径前缀也没有版本请求头时，是否回退到默认后端。
     */
    @Test
    void testDefaultVersionFallback() {
        // 1. 配置过滤器：设置默认回退后端
        ApiVersioningGatewayFilterFactory.Config config = new ApiVersioningGatewayFilterFactory.Config();
        config.setUsePathPrefix(false); // 不使用路径前缀
        config.setDefaultBackend("http://default-service/v1");
        GatewayFilter filter = filterFactory.apply(config);

        // 2. 模拟请求：创建一个没有任何版本信息的请求
        MockServerHttpRequest mockRequest = MockServerHttpRequest.get("http://localhost/api/profile")
                .build();
        MockServerWebExchange mockExchange = MockServerWebExchange.from(mockRequest);

        // 3. 模拟 GatewayFilterChain
        GatewayFilterChain mockChain = mock(GatewayFilterChain.class);
        when(mockChain.filter(any())).thenReturn(Mono.empty());

        // 4. 应用过滤器并订阅
        filter.filter(mockExchange, mockChain).block();

        // 5. 验证结果
        verify(mockChain).filter(any());
        URI rewrittenUri = mockExchange.getRequest().getURI();

        // 期望的 URI 应该是 "http://default-service/v1/api/profile"
        String expectedUri = "http://default-service/v1/api/profile";
        assertThat(rewrittenUri.toString()).isEqualTo(expectedUri);
    }
}
