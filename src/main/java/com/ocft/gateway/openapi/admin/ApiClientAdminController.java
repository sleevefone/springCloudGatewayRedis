package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin API for managing API Clients (AppKey/SecretKey pairs).
 */
@RestController
@RequestMapping("/admin/api-clients")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.admin", name = "enabled", havingValue = "true")
public class ApiClientAdminController {

    private final ApiClientService apiClientService;

    /**
     * Get all API clients, with optional filtering by a query string.
     * @param query An optional string to filter clients by appKey or description.
     * @return A list of API clients.
     */
    @GetMapping
    public List<ApiClient> getAllApiClients(@RequestParam(value = "query", required = false) String query) {
        return apiClientService.getAllClients(query);
    }

    /**
     * Create a new API client.
     * AppKey and SecretKey are generated automatically.
     * @param apiClientPayload A DTO containing the description for the new client.
     * @return The newly created client with its generated keys.
     */
    @PostMapping
    public ResponseEntity<ApiClient> createApiClient(@RequestBody ApiClient apiClientPayload) {
        ApiClient newClient = new ApiClient();
        newClient.setDescription(apiClientPayload.getDescription());
        newClient.setEnabled(true);

        // Generate secure, random keys
        newClient.setAppKey("AK" + UUID.randomUUID().toString().replace("-", ""));
        newClient.setSecretKey("SK" + UUID.randomUUID().toString().replace("-", ""));

        ApiClient savedClient = apiClientService.saveClient(newClient);
        return new ResponseEntity<>(savedClient, HttpStatus.CREATED);
    }

    /**
     * Update an existing API client (e.g., to change description or enabled status).
     * @param id The ID of the client to update.
     * @param apiClientDetails The new details for the client.
     * @return The updated client.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiClient> updateApiClient(@PathVariable Long id, @RequestBody ApiClient apiClientDetails) {
        return apiClientService.getClientById(id)
                .map(existingClient -> {
                    existingClient.setDescription(apiClientDetails.getDescription());
                    existingClient.setEnabled(apiClientDetails.isEnabled());
                    ApiClient updatedClient = apiClientService.saveClient(existingClient);
                    return ResponseEntity.ok(updatedClient);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete an API client.
     * @param id The ID of the client to delete.
     * @return A response entity indicating success or failure.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApiClient(@PathVariable Long id) {
        apiClientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
