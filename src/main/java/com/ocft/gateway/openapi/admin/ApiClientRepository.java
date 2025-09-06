package com.ocft.gateway.openapi.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for the ApiClient entity.
 */
@Repository
public interface ApiClientRepository extends JpaRepository<ApiClient, Long> {

    /**
     * Finds an active API client by its unique appKey.
     * This will be the primary method used by the authentication filter.
     * @param appKey The appKey to search for.
     * @return An Optional containing the found ApiClient, or empty if not found.
     */
    Optional<ApiClient> findByAppKey(String appKey);
}
