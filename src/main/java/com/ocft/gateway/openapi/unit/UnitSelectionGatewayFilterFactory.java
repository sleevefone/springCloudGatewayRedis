

package com.ocft.gateway.openapi.unit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * 第 1 步：在 L1 网关中定义一个“通配”路由在您的路由管理服务 (route-manager) 中，为 L1 网关创建这样一条路由规则。注意，它的 uri 是一个无意义的占位符，因为它会被我们的自定义 Filter 覆盖。
 * <p>
 * {
 * "id": "dynamic-unit-routing-rule",
 * "uri": "lb://placeholder-service", // 占位符，无实际意义
 * "order": 100,
 * "predicates": [
 * {
 * "name": "Path",
 * "args": {
 * "patterns": "/api/**" // 匹配所有需要单元化路由的 API 请求
 * }
 * }
 * ],
 * "filters": [
 * {
 * "name": "UnitSelection", // 这是我们即将创建的自定义 Filter 的名字
 * "args": {}
 * }
 * ]
 * }
 * 第 2 步：在 L1 网关项目中编写自定义 GatewayFilterFactory您需要在 L1 网关的代码中创建一个新的 Java 类。
 * <p>
 * 第 3 步：性能优化 - 引入高速缓存直接在 Filter 里查询数据库是绝对不可行的，它会拖垮整个网关。您必须在 mappingService 中实现一个高速缓存（例如使用 Caffeine 或 Guava Cache）。•缓存逻辑: getUnitByTenantId 方法应该先查缓存，如果缓存中没有，才去查数据库，并将结果放入缓存（设置一个合理的过期时间，比如 5 分钟）。•缓存更新: 当数据库中的映射关系变化时，需要有机制来使缓存失效（比如通过消息队列通知网关清除缓存）
 * <p>
 * 第三层就是您的业务微服务集群（order-service, user-service 等），而 L2 网关正是通过注册中心 (Service Discovery) 来找到它们的。我们来把这个流程的最后一块拼图放上：角色分工：L2 网关与注册中心•L2 网关 (In-Unit Gateway): 它是单元内部的“交通警察”。它的职责是根据请求的路径 (/api/orders/**)，决定这个请求应该交给哪个业务团队（比如“订单服务团队”）处理。但它只知道团队的名字 (order-service-in-unit-a)，并不知道这个团队的具体成员（IP 地址和端口）在哪里。•注册中心 (Service Discovery, e.g., Nacos, Eureka, Consul): 它是整个单元的“通讯录”或“动态地图”。每个微服务实例在启动时，都会向注册中心报告自己的地址（“我是‘订单服务团队’的张三，我的地址是 10.10.1.5:8080”）。L2 网关的路由规则 (关键点)L2 网关的路由规则和 L1 的有一个本质区别，就是 uri 的格式。它会使用 lb:// 协议，lb 代表 Load Balancer (负载均衡)。在 L2 网关的路由配置中，规则是这样的：
 * {
 * "id": "route-to-orders-service-in-unit-a",
 * "uri": "lb://order-service-in-unit-a", // 注意这里！
 * "order": 1,
 * "predicates": [
 * {
 * "name": "Path",
 * "args": {
 * "patterns": "/api/orders/**"
 * }
 * }
 * ]
 * }
 * <p>
 * 把 L1, L2, L3 和注册中心串联起来，看一个完整的请求旅程：
 * 1.[外部请求] -> 客户端发送请求 GET /api/orders/123，并携带分区键 X-Tenant-ID: tenant-1。
 * 2.[L1 网关] -> a. L1 网关的自定义 Filter (UnitSelectionGatewayFilter) 捕获请求。 b. Filter 提取 tenant-1，查询缓存/数据库，得知 tenant-1 属于 LA 单元。 c. Filter 从配置中找到 LA 单元对应的 L2 网关地址 http://l2-gateway-of-unit-a.com。 d. L1 网关动态修改请求目标，将请求转发给 L2 网关。
 * 3.[L2 网关 (LA 单元)] -> a. L2 网关收到请求 GET /api/orders/123。 b. 它在自己的路由表（从它自己的 Redis 加载）中进行匹配，根据路径 /api/orders/** 找到了上面那条规则。 c. 它看到了目标是 uri: "lb://order-service-in-unit-a"。
 * 4.[L2 网关与注册中心交互] -> a. Spring Cloud Gateway 的服务发现模块被激活。 b. L2 网关向注册中心发出查询：“你好，请告诉我 order-service-in-unit-a 这个服务现在有哪些健康的实例？” c. 注册中心回复一个地址列表，比如 [10.10.1.5:8080, 10.10.1.6:8080]。
 * 5.[L2 网关执行负载均衡] -> a. Spring Cloud Gateway 内置的负载均衡器 (Spring Cloud LoadBalancer) 从列表中选择一个实例，比如 10.10.1.5:8080（默认使用轮询策略）。 b. L2 网关将请求最终转发到 http://10.10.1.5:8080/api/orders/123。
 * 6.[L3 微服务] -> order-service-in-unit-a 的某个实例接收到请求，执行业务逻辑，然后将响应原路返回。
 */
@Slf4j
@Component
public class UnitSelectionGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public static final String DYNAMIC = "dynamic_";
    private final TenantUnitMappingService mappingService;

    // 使用构造函数注入，这是推荐的最佳实践
    public UnitSelectionGatewayFilterFactory(TenantUnitMappingService mappingService) {
        this.mappingService = mappingService;
    }
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
            URI originalUri = exchange.getRequest().getURI();

            if (tenantId == null) {
                log.trace("No X-Tenant-ID header found, skipping dynamic routing for: {}", originalUri);
                return chain.filter(exchange);
            }

            String unit = mappingService.getUnitByTenantId(tenantId);
            log.debug("Tenant '{}' mapped to unit '{}'", tenantId, unit);

            String l2GatewayHost = lookupL2GatewayHostFromConfig(unit);

            if (l2GatewayHost == null) {
                log.warn("No L2 gateway mapping found for tenant '{}'. Passing through.", tenantId);
                return chain.filter(exchange);
            }

            URI newUri = UriComponentsBuilder.fromUriString(l2GatewayHost)
                    .path(originalUri.getRawPath())
                    .query(originalUri.getRawQuery())
                    .build(true)
                    .toUri();

            // 关键: 必须同时覆盖两个属性
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, newUri);
            ServerWebExchangeUtils.addOriginalRequestUrl(exchange, originalUri);

            Route newRoute = Route.async()
                    .id(DYNAMIC + l2GatewayHost) // 路由 ID
                    .uri(newUri) // 目标 URI
                    .predicate(x -> true) // 匹配规则，这里写死匹配所有
                    .build();
            // 覆盖路由目标，避免继续使用占位符 route
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR,newRoute);

            log.info("Dynamic routing for tenant '{}': {} -> {}", tenantId, originalUri, newUri);

            return chain.filter(exchange);
        };
    }


    // 伪代码：从配置中读取 L2 网关的主机地址
    private String lookupL2GatewayHostFromConfig(String unit) {
        // 这里应该只返回 host，例如 "http://l2-gateway-of-unit-a.com" 或 "https://httpbin.org"
        // 在实际应用中，这部分逻辑会更复杂，可能需要从配置中心（如 Nacos, Apollo）读取
        if ("LA".equals(unit)) { // 为了方便您的测试
            return "https://httpbin.org";
        }
        return null;
    }
}