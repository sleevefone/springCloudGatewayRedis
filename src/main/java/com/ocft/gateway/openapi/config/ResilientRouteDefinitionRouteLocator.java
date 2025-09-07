//package com.ocft.gateway.openapi.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.config.GatewayProperties;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
//import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
//import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
//import org.springframework.cloud.gateway.route.Route;
//import org.springframework.cloud.gateway.route.RouteDefinition;
//import org.springframework.cloud.gateway.support.ConfigurationService;
//import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
//import org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator;
//import org.springframework.context.annotation.Primary;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * A resilient implementation of a Route Locator.
// * This class overrides the default Spring Cloud Gateway behavior to prevent the entire application
// * from failing to start if a single route definition is malformed or invalid.
// * It wraps the route conversion logic in a try-catch block, logging errors for invalid routes
// * and skipping them, allowing the gateway to start with the valid routes.
// */
//@Component
//@Primary
//@Slf4j
//public class ResilientRouteDefinitionRouteLocator extends RouteDefinitionRouteLocator {
//
//    public ResilientRouteDefinitionRouteLocator(RouteDefinitionLocator routeDefinitionLocator,
//    List<RoutePredicateFactory> predicates, List<GatewayFilterFactory> gatewayFilterFactories,
//    GatewayProperties gatewayProperties, ConfigurationService configurationService) {
//        super(routeDefinitionLocator, predicates, gatewayFilterFactories, gatewayProperties, configurationService);
//    }
//
//    /**
//     * Overrides the default method to provide resilience against individual route conversion errors.
//     * @param routeDefinition The route definition to convert.
//     * @return A Mono containing the converted Route, or an empty Mono if conversion fails.
//     */
//    @Override
//    public Flux<Route> getRoutesByMetadata(Map<String, Object> metadata) {
//        // Use the reactive error handling operator `onErrorResume`.
//        // This is the idiomatic way to handle errors in a reactive stream.
//        return super.convertToRoute(routeDefinition)
//            .onErrorResume(e -> {
//                // If an error occurs during conversion, log it and return an empty Mono.
//                // This effectively skips the invalid route without terminating the entire stream.
//                log.error("Failed to load route definition with id: '{}'. Reason: {}. Skipping this route.",
//                          routeDefinition.getId(), e.getMessage(), e);
//                return Mono.empty();
//            });
//    }
//}
