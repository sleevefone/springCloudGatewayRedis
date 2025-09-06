
test: org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory.apply
biz:　org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory.traceMatch

　　│
　　▼
1. 路由匹配: 引擎发现请求匹配了 `id="configurable_filter_test"` 的路由。
   │
   ▼
2. 查找过滤器配置: 引擎看到该路由的 `filters` 列表里有:
   `{ "name": "AddTimestamp", "args": { "headerName": "X-My-Timestamp" } }`
   │
   ▼
3. **调用工厂 (核心步骤)**:
   a. 引擎根据 `name` 找到名为 `"AddTimestamp"` 的工厂 (就是你的类)。
   b. 引擎创建一个空的 `Config` 对象。
   c. 引擎读取 `args`，并调用 `config.setHeaderName("X-My-Timestamp")` 来填充 `Config` 对象。
   d. 引擎调用工厂的 `apply(config)` 方法，并将填充好的 `Config` 对象传进去。
   │
   ▼
4. **生产过滤器**: `apply` 方法执行，返回一个包含了 `headerName="X-My-Timestamp"` 这个上下文信息的 `GatewayFilter` 实例。
   │
   ▼
5. **执行过滤器**: 这个新生产的 `GatewayFilter` 被放入当前请求的过滤器链中，并执行它的逻辑（添加请求头）。
   │
   ▼