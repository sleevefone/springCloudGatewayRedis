package com.ocft.gateway.openapi.admin;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "gateway_routes")
@Data
public class RouteDefinitionEntity {

    @Id
    @Column(length = 100)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String uri;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String predicates;

    @Column(columnDefinition = "TEXT")
    private String filters;

    @Column(nullable = false)
    private int routeOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;
}
