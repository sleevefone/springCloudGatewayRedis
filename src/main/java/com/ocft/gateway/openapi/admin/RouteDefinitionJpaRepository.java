package com.ocft.gateway.openapi.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteDefinitionJpaRepository extends JpaRepository<RouteDefinitionEntity, String> {

    List<RouteDefinitionEntity> findByEnabled(boolean enabled);

    /**
     * Finds routes where the ID or URI contains the given query string, ignoring case.
     * @param id The string to search for in the route ID.
     * @param uri The string to search for in the route URI.
     * @return A list of matching route entities.
     */
    List<RouteDefinitionEntity> findByIdContainingIgnoreCaseOrUriContainingIgnoreCase(String id, String uri);

}
