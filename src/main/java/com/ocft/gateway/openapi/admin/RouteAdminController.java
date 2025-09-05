package com.ocft.gateway.openapi.admin;

import com.ocft.gateway.openapi.config.RedisRouteDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 用于动态管理网关路由的 REST 控制器。
 * <p>
 * 这些端点应该被安全措施（如 Spring Security）保护，以防止未经授权的访问。
 * </p>
 */
@RestController
@RequestMapping("/admin/routes") // 将所有管理 API 放在 /admin/routes 路径下
@RequiredArgsConstructor
@Slf4j
public class RouteAdminController {

    private final RedisRouteDefinitionRepository routeDefinitionRepository;

    /**
     * 添加或更新一个路由定义。
     */
    @PostMapping
    public Mono<ResponseEntity<String>> saveRoute(@RequestBody RouteDefinition definition) {
        log.info("Admin request to add/update route: [{}]", definition.getId());
        return routeDefinitionRepository.save(Mono.just(definition))
                .then(Mono.just(ResponseEntity.ok("Route '" + definition.getId() + "' was saved.")))
                .onErrorResume(e -> {
                    log.error("Error saving route [{}]: {}", definition.getId(), e.getMessage(), e);
                    return Mono.just(ResponseEntity.status(500).body("Error saving route: " + e.getMessage()));
                });
    }

    /**
     * 根据 ID 删除一个路由。
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> deleteRoute(@PathVariable String id) {
        log.info("Admin request to delete route: [{}]", id);
        return routeDefinitionRepository.delete(Mono.just(id))
                .then(Mono.just(ResponseEntity.ok("Route '" + id + "' was deleted.")));
    }

    /**
     * 获取所有当前的路由定义。
     */
    @GetMapping
    public Flux<RouteDefinition> getAllRoutes() {
        log.info("Admin request to get all routes.");
        return routeDefinitionRepository.getRouteDefinitions();
    }
}