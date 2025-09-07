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
//import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
//import org.springframework.cloud.gateway.route.RouteDefinitionRouteLocator;
//import org.springframework.context.annotation.Primary;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
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
//    public ResilientRouteDefinitionRouteLocator(
//            RouteDefinitionLocator routeDefinitionLocator,
//            List<RoutePredicateFactory> predicateFactories,
//            List<GatewayFilterFactory> filterFactories,
//            GatewayProperties gatewayProperties) {
//        super(routeDefinitionLocator, predicateFactories, filterFactories, gatewayProperties);
//    }
//
//    /**
//     * Overrides the default method to provide resilience against individual route conversion errors.
//     * @param routeDefinition The route definition to convert.
//     * @return A Mono containing the converted Route, or an empty Mono if conversion fails.
//     */
//    @Override
//    protected Mono<Route> convertToRoute(RouteDefinition routeDefinition) {
//        try {
//            // Attempt to convert the route definition as normal.
//            return super.convertToRoute(routeDefinition);
//        } catch (Exception e) {
//            // If any exception occurs during conversion (e.g., predicate name not found),
//            // log the error and return an empty Mono.
//            // This prevents a single bad route from crashing the entire application startup.
//            log.error("Failed to load route definition with id: '{}'. Reason: {}. Skipping this route.",
//                      routeDefinition.getId(), e.getMessage());
//            return Mono.empty();
//        }
//    }
//}
