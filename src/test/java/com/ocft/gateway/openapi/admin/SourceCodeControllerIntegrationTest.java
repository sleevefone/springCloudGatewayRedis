package com.ocft.gateway.openapi.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration test for the SourceCodeController.
 * This test starts a full Spring Boot application context to verify the entire flow
 * from the controller down to the service reading a real file from the test resources.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SourceCodeControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whenRequestingSourceForExistingClass_thenReturnsSourceCode() {
        // Arrange: The class we want to find the source for.
        String targetClassName = "com.ocft.gateway.openapi.admin.test.DummyFactory";
        String expectedSourceCode = "package com.ocft.gateway.openapi.admin.test;\n\n" +
                                    "// This is a dummy file for integration testing the source code reading feature.\n" +
                                    "public class DummyFactory {\n" +
                                    "    public String getName() {\n" +
                                    "        return \"Dummy\";\n" +
                                    "    }\n" +
                                    "}\n";

        // Act & Assert: Perform the API call and verify the response.
        webTestClient.get()
                .uri("/admin/factories/source?className=" + targetClassName)
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String.class).isEqualTo(expectedSourceCode);
    }

    @Test
    void whenRequestingSourceForNonExistentClass_thenReturnsNotFoundMessage() {
        // Arrange
        String targetClassName = "com.example.NonExistentClass";
        String expectedMessage = String.format("Source code for class '%s' not found in classpath.", targetClassName);

        // Act & Assert
        webTestClient.get()
                .uri("/admin/factories/source?className=" + targetClassName)
                .exchange()
                .expectStatus().isOk() // The service returns a message with 200 OK status
                .expectBody(String.class).isEqualTo(expectedMessage);
    }
}
