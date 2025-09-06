package com.ocft.gateway.openapi.admin;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 路由管理服务接口
 */
public interface RouteAdminService {

    /**
     * 从数据库获取所有路由定义 (以Payload格式)
     * @return A Flux of route definition payloads
     */
    Flux<RouteDefinitionPayload> getAllRoutes();

    /**
     * 保存路由（新建或更新）到数据库和Redis
     * @param payload 路由定义负载
     * @return a Mono that completes when the save operation is finished
     */
    Mono<Void> save(RouteDefinitionPayload payload);

    /**
     * 从数据库和Redis中删除路由
     * @param routeId 路由ID
     * @return a Mono that completes when the delete operation is finished
     */
    Mono<Void> delete(String routeId);

    Mono<RouteDefinitionPayload> getById(String routeId);

    /**
     * 手动触发一次全局路由刷新事件
     */
    void refreshRoutes();
}
