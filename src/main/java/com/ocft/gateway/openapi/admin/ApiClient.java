package com.ocft.gateway.openapi.admin;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Represents a client application that is authorized to access the gateway.
 * This entity stores the credentials used for signature authentication.
 */
@Entity
@Table(name = "api_clients")
@Data
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String appKey;

    @Column(nullable = false)
    private String secretKey;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;
}
