package com.ocft.gateway.openapi.admin;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

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

    @Column(nullable = false)
    private String predicateDescription;

    @Column(nullable = false)
    private String filterDescription;

    @Column(nullable = false)
    private String creator;

    @Column(nullable = false)
    private String updater;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
