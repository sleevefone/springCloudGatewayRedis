package com.ocft.gateway.openapi.admin;

import lombok.Data;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for API endpoints to correctly capture the full route definition from the frontend,
 * including the 'enabled' state of filters and the route itself.
 */
@Data
public class RouteDefinitionPayload {

    private String id;

    private String uri;

    private int order = 0;

    private boolean enabled;

    private List<PredicateDefinition> predicates = new ArrayList<>();

    private List<FilterInfo> filters = new ArrayList<>();

    // New fields for descriptions
    private String predicateDescription;

    private String filterDescription;
}
