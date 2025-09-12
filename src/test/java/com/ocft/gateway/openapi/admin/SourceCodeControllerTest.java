package com.ocft.gateway.openapi.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;

/**
 * Unit tests for the SourceCodeController.
 * It uses @WebFluxTest to only load the web layer and mocks all service dependencies.
 */
class SourceCodeControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    // **CRITICAL FIX: Mock ALL dependencies required by the controller.**
    @MockBean
    private GatewayFilterService gatewayFilterService;
    

    @Test
    void whenFilterExists_getFilterSourceCode_thenReturnOkWithSourceCode() throws IOException {
        // Arrange: Define the test data and mock behavior
        String filterShortName = "AddRequestHeader";
        String filterClassName = "org.springframework.cloud.gateway.filter.factory.AddRequestHeaderGatewayFilterFactory";
        String sourceCode = "public class AddRequestHeaderGatewayFilterFactory { /* ... source ... */ }";

        // 1. Mock the GatewayFilterService to return the factory info
        GatewayFilterService.FactoryInfo factoryInfo = new GatewayFilterService.FactoryInfo(filterShortName, filterClassName, Collections.emptyList());
        given(gatewayFilterService.getAvailableFilters()).willReturn(List.of(factoryInfo));

        // 2. Mock the SourceCodeService to return the source code when called with the correct class name

        // Act & Assert: Perform the API call and verify the response
        webTestClient.get().uri("/code/filter/sourceCode?name=" + filterShortName)
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo(sourceCode);
    }

    @Test
    void whenFilterDoesNotExist_getFilterSourceCode_thenReturnNotFound() {
        // Arrange: Mock the GatewayFilterService to return an empty list
        String filterShortName = "NonExistentFilter";
        given(gatewayFilterService.getAvailableFilters()).willReturn(Collections.emptyList());

        // Act & Assert: Perform the API call and verify the 404 response
        webTestClient.get().uri("/code/filter/sourceCode?name=" + filterShortName)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class).isEqualTo("Filter factory not found: " + filterShortName);
    }

    @Test
    void whenServiceThrowsIOException_getFilterSourceCode_thenReturnInternalServerError() throws IOException {
        // Arrange
        String filterShortName = "CorruptedFilter";
        String filterClassName = "com.example.CorruptedFilterFactory";
        String exceptionMessage = "Disk read error";

        // 1. Mock the GatewayFilterService
        GatewayFilterService.FactoryInfo factoryInfo = new GatewayFilterService.FactoryInfo(filterShortName, filterClassName, Collections.emptyList());
        given(gatewayFilterService.getAvailableFilters()).willReturn(List.of(factoryInfo));

        // Act & Assert
        webTestClient.get().uri("/code/filter/sourceCode?name=" + filterShortName)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(String.class).isEqualTo("Error reading source code: " + exceptionMessage);
    }
}
