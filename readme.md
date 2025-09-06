### 1：后管地址: http://localhost:8888/index.html
### 2：后管示例：
![bankend.png](bankend.png)
### 3：数据库脚本
```text
CREATE TABLE `gateway_routes` (
  `id` varchar(100) NOT NULL,
  `uri` text NOT NULL,
  `predicates` text NOT NULL,
  `filters` text,
  `route_order` int NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB

```
### 单元化，双网关思路
```text
 第 1 步：在 L1 网关中定义一个“通配”路由在您的路由管理服务 (route-manager) 中，为 L1 网关创建这样一条路由规则。注意，它的 uri 是一个无意义的占位符，因为它会被我们的自定义 Filter 覆盖。
 
 {
 "id": "dynamic-unit-routing-rule",
 "uri": "lb://placeholder-service", // 占位符，无实际意义
 "order": 100,
 "predicates": [
 {
 "name": "Path",
 "args": {
 "patterns": "/api/**" // 匹配所有需要单元化路由的 API 请求
 }
 }
 ],
 "filters": [
 {
 "name": "UnitSelection", // 这是我们即将创建的自定义 Filter 的名字
 "args": {}
 }
 ]
 }
 第 2 步：在 L1 网关项目中编写自定义 GatewayFilterFactory您需要在 L1 网关的代码中创建一个新的 Java 类。
 
 第 3 步：性能优化 - 引入高速缓存直接在 Filter 里查询数据库是绝对不可行的，它会拖垮整个网关。您必须在 mappingService 中实现一个高速缓存（例如使用 Caffeine 或 Guava Cache）。•缓存逻辑: getUnitByTenantId 方法应该先查缓存，如果缓存中没有，才去查数据库，并将结果放入缓存（设置一个合理的过期时间，比如 5 分钟）。•缓存更新: 当数据库中的映射关系变化时，需要有机制来使缓存失效（比如通过消息队列通知网关清除缓存）
 
 第三层就是您的业务微服务集群（order-service, user-service 等），而 L2 网关正是通过注册中心 (Service Discovery) 来找到它们的。我们来把这个流程的最后一块拼图放上：角色分工：L2 网关与注册中心•L2 网关 (In-Unit Gateway): 它是单元内部的“交通警察”。它的职责是根据请求的路径 (/api/orders/**)，决定这个请求应该交给哪个业务团队（比如“订单服务团队”）处理。但它只知道团队的名字 (order-service-in-unit-a)，并不知道这个团队的具体成员（IP 地址和端口）在哪里。•注册中心 (Service Discovery, e.g., Nacos, Eureka, Consul): 它是整个单元的“通讯录”或“动态地图”。每个微服务实例在启动时，都会向注册中心报告自己的地址（“我是‘订单服务团队’的张三，我的地址是 10.10.1.5:8080”）。L2 网关的路由规则 (关键点)L2 网关的路由规则和 L1 的有一个本质区别，就是 uri 的格式。它会使用 lb:// 协议，lb 代表 Load Balancer (负载均衡)。在 L2 网关的路由配置中，规则是这样的：
 {
 "id": "route-to-orders-service-in-unit-a",
 "uri": "lb://order-service-in-unit-a", // 注意这里！
 "order": 1,
 "predicates": [
 {
 "name": "Path",
 "args": {
 "patterns": "/api/orders/**"
 }
 }
 ]
 }
 
 把 L1, L2, L3 和注册中心串联起来，看一个完整的请求旅程：
 1.[外部请求] -> 客户端发送请求 GET /api/orders/123，并携带分区键 X-Tenant-ID: tenant-1。
 2.[L1 网关] -> a. L1 网关的自定义 Filter (UnitSelectionGatewayFilter) 捕获请求。 b. Filter 提取 tenant-1，查询缓存/数据库，得知 tenant-1 属于 LA 单元。 c. Filter 从配置中找到 LA 单元对应的 L2 网关地址 http://l2-gateway-of-unit-a.com。 d. L1 网关动态修改请求目标，将请求转发给 L2 网关。
 3.[L2 网关 (LA 单元)] -> a. L2 网关收到请求 GET /api/orders/123。 b. 它在自己的路由表（从它自己的 Redis 加载）中进行匹配，根据路径 /api/orders/** 找到了上面那条规则。 c. 它看到了目标是 uri: "lb://order-service-in-unit-a"。
 4.[L2 网关与注册中心交互] -> a. Spring Cloud Gateway 的服务发现模块被激活。 b. L2 网关向注册中心发出查询：“你好，请告诉我 order-service-in-unit-a 这个服务现在有哪些健康的实例？” c. 注册中心回复一个地址列表，比如 [10.10.1.5:8080, 10.10.1.6:8080]。
 5.[L2 网关执行负载均衡] -> a. Spring Cloud Gateway 内置的负载均衡器 (Spring Cloud LoadBalancer) 从列表中选择一个实例，比如 10.10.1.5:8080（默认使用轮询策略）。 b. L2 网关将请求最终转发到 http://10.10.1.5:8080/api/orders/123。
 6.[L3 微服务] -> order-service-in-unit-a 的某个实例接收到请求，执行业务逻辑，然后将响应原路返回。
```