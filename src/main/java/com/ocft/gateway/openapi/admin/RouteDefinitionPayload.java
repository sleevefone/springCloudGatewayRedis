package com.ocft.gateway.openapi.admin;

import lombok.Data;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for API endpoints to correctly capture the full route definition from the frontend,
 * including the 'enabled' state of filters.
 */
@Data
public class RouteDefinitionPayload {

    private String id;

    private String uri;

    private int order = 0;

    private List<PredicateDefinition> predicates = new ArrayList<>();

    private List<FilterInfo> filters = new ArrayList<>();
}
