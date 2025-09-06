package com.ocft.gateway.openapi.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteDefinitionJpaRepository extends JpaRepository<RouteDefinitionEntity, String> {

    List<RouteDefinitionEntity> findByEnabled(boolean enabled);
}