package com.ocft.gateway.openapi.admin;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing API Clients.
 */
public interface ApiClientService {

    /**
     * Retrieves an API client by its appKey.
     * This is a key method for the authentication process.
     * @param appKey The appKey of the client.
     * @return An Optional containing the ApiClient if found.
     */
    Optional<ApiClient> getByAppKey(String appKey);

    /**
     * Retrieves all API clients.
     * @return A list of all clients.
     */
    List<ApiClient> getAllClients();

    /**
     * Saves or updates an API client.
     * @param client The client to save.
     * @return The saved client.
     */
    ApiClient saveClient(ApiClient client);

    /**
     * Deletes an API client by its ID.
     * @param id The ID of the client to delete.
     */
    void deleteClient(Long id);
}
