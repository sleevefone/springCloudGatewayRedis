package com.ocft.gateway.openapi.config;

import com.ocft.gateway.openapi.admin.GatewayFilterService;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RouteInspectorService {

    private final RouteDefinitionRepository routeDefinitionRepository;
    private final GatewayFilterService gatewayFilterService;

    public RouteInspectorService(RouteDefinitionRepository routeDefinitionRepository, GatewayFilterService gatewayFilterService) {
        this.routeDefinitionRepository = routeDefinitionRepository;
        this.gatewayFilterService = gatewayFilterService;
    }

    public Flux<RouteDefinition> getCurrentRouteDefinitions() {
        return routeDefinitionRepository.getRouteDefinitions()
                .filter(routeDefinition -> {
                    // 同上，验证 Predicates 和 Filters
                    List<PredicateDefinition> predicates = routeDefinition.getPredicates();
                    List<FilterDefinition> filters = routeDefinition.getFilters();
                    Set<String> predicateNames = predicates.stream()
                            .map(PredicateDefinition::getName)
                            .collect(Collectors.toSet());
                    Set<String> filterNames = filters.stream()
                            .map(FilterDefinition::getName)
                            .collect(Collectors.toSet());
                    predicateNames.retainAll(gatewayFilterService.getAvailablePredicateNames());
                    filterNames.retainAll(gatewayFilterService.getAvailableFilterNames());
                    return !predicateNames.isEmpty() && !filterNames.isEmpty();
                });
    }
}