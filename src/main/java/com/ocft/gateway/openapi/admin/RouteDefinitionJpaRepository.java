package com.ocft.gateway.openapi.admin;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteDefinitionJpaRepository extends JpaRepository<RouteDefinitionEntity, String> {
}