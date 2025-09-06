package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * 后台管理API，用于动态管理路由规则
 * 提供给前端页面调用
 */
@RestController
@RequestMapping("/admin/routes")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "gateway.admin", name = "enabled", havingValue = "true")
public class RouteAdminController {

    private final RouteAdminService routeAdminService;

    /**
     * 获取所有路由定义 (以Payload格式返回给前端)
     * @return A Flux of route definition payloads
     */
    @GetMapping
    public Flux<RouteDefinitionPayload> getAllRoutes() {
        return routeAdminService.getAllRoutes();
    }

    /**
     * 创建一个新的路由
     * @param payload 路由定义负载
     * @return 成功或失败
     */
    @PostMapping
    public Mono<ResponseEntity<Object>> createRoute(@RequestBody RouteDefinitionPayload payload) {
        return routeAdminService.save(payload)
                .then(Mono.just(ResponseEntity.created(URI.create("/admin/routes/" + payload.getId())).build()))
                .onErrorResume(e -> {
                    log.error("Failed to create route: {}", payload.getId(), e);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create route: " + e.getMessage()));
                });
    }

    /**
     * 更新一个已有的路由
     * @param id 路由ID
     * @param payload 路由定义负载
     * @return 成功或失败
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Void>> updateRoute(@PathVariable String id, @RequestBody RouteDefinitionPayload payload) {
        // 确保ID一致
        if (payload.getId() == null || !id.equals(payload.getId())) {
            payload.setId(id);
        }
        return routeAdminService.save(payload)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    /**
     * 删除一个路由
     * @param id 路由ID
     * @return 成功或失败
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteRoute(@PathVariable String id) {
        return routeAdminService.delete(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
    /**
     * 根据ID获取单个路由定义
     * @param id 路由ID
     * @return 路由定义或404 Not Found
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<RouteDefinitionPayload>> getById(@PathVariable String id) {
        return routeAdminService.getById(id)
                // 使用 map 将成功获取的 payload 包装成 200 OK 响应
                .map(ResponseEntity::ok)
                // 如果上游 Mono 为空 (即未找到路由)，则返回 404 Not Found
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    /**
     * 手动触发一次全局路由刷新
     * @return
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<Void>> refreshRoutes() {
        routeAdminService.refreshRoutes();
        return Mono.just(ResponseEntity.ok().build());
    }
}
