package com.ocft.gateway.openapi.filter;

import com.ocft.gateway.openapi.admin.ApiClient;
import com.ocft.gateway.openapi.admin.ApiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * A GatewayFilterFactory that enforces signature authentication for API requests.
 * This is a best-practice implementation for securing gateway access.
 */
@Component
@Slf4j
public class SignatureAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final ApiClientService apiClientService;

    // Using constructor injection for the service dependency
    public SignatureAuthenticationGatewayFilterFactory(ApiClientService apiClientService) {
        super(Object.class);
        this.apiClientService = apiClientService;
    }

    // Define the names of the required HTTP headers for authentication
    private static final String APP_KEY_HEADER = "X-AppKey";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final String NONCE_HEADER = "X-Nonce"; // A random string to prevent replay attacks
    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final long TIMESTAMP_VALIDITY_SECONDS = 300; // 5 minutes validity window

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            HttpHeaders headers = request.getHeaders();

            // Step 1: Check for the presence of all required authentication headers.
            String appKey = headers.getFirst(APP_KEY_HEADER);
            String timestampStr = headers.getFirst(TIMESTAMP_HEADER);
            String nonce = headers.getFirst(NONCE_HEADER);
            String clientSignature = headers.getFirst(SIGNATURE_HEADER);

            if (appKey == null || timestampStr == null || nonce == null || clientSignature == null) {
                return unauthorized(exchange, "Missing required authentication headers.");
            }

            // Step 2: Validate the timestamp to prevent replay attacks.
            long timestamp;
            try {
                timestamp = Long.parseLong(timestampStr);
            } catch (NumberFormatException e) {
                return unauthorized(exchange, "Invalid timestamp format.");
            }

            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            if (Math.abs(currentTimeSeconds - timestamp) > TIMESTAMP_VALIDITY_SECONDS) {
                return unauthorized(exchange, "Timestamp is expired or invalid.");
            }

            // Step 3: Fetch the API Client from the database using the provided AppKey.
            // Note: In a high-traffic environment, this result should be cached (e.g., in Redis or Caffeine).
            // This is the corrected, fully reactive chain that properly handles blocking I/O.
            return Mono.fromCallable(() -> apiClientService.getByAppKey(appKey))
                    .subscribeOn(Schedulers.boundedElastic()) // Offload the blocking database call
                    .flatMap(Mono::justOrEmpty) // Convert Mono<Optional<ApiClient>> to an empty or single-item Mono<ApiClient>
                    .flatMap(apiClient -> {
                        if (!apiClient.isEnabled()) {
                            return unauthorized(exchange, "API client is disabled.");
                        }
                        // If client is found and enabled, proceed to the cryptographic signature verification.
                        return verifySignature(exchange, chain, apiClient, clientSignature);
                    })
                    // If no client is found for the given AppKey, reject the request.
                    .switchIfEmpty(unauthorized(exchange, "Invalid AppKey."))
                    .doOnError(e -> log.error("Error during signature authentication process", e));
        };
    }

    /**
     * The core logic for signature verification. It caches the request body to read it,
     * then reconstructs the string-to-sign and compares the calculated signature.
     */
    private Mono<Void> verifySignature(ServerWebExchange exchange, GatewayFilterChain chain, ApiClient client, String clientSignature) {
        // Caching the request body is essential because it can only be read once in a reactive stream.
        return ServerWebExchangeUtils.cacheRequestBody(exchange, serverHttpRequest -> {
            // Step 4: Asynchronously read the cached request body into a single string.
            Mono<String> bodyMono = serverHttpRequest.getBody()
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return new String(bytes, StandardCharsets.UTF_8);
                    })
                    .collectList()
                    .map(list -> String.join("", list))
                    .defaultIfEmpty("");

            // Step 5: Once the body is read, proceed with signature verification.
            return bodyMono.flatMap(body -> {
                // Construct the string-to-sign on the server side.
                String stringToSign = buildStringToSign(serverHttpRequest, body);
                log.debug("Server-side string to sign: \n{}", stringToSign);

                // Calculate the expected signature using the client's secret key.
                String serverSignature = calculateHmacSha256(stringToSign, client.getSecretKey());
                log.debug("Server signature: {}, Client signature: {}", serverSignature, clientSignature);

                // Step 6: Compare the client's signature with the one we calculated.
                if (Objects.equals(serverSignature, clientSignature)) {
                    // Success! The request is authentic. Pass it to the next filter in the chain.
                    // 使用传入的 'chain' 对象，而不是 exchange.getFilterChain()
                    return chain.filter(exchange.mutate().request(serverHttpRequest).build());
                } else {
                    // Failure! The signatures do not match.
                    return unauthorized(exchange, "Signature mismatch.");
                }
            });
        });
    }

    /**
     * Constructs the canonical string that will be used for signing.
     * The order and format are critical and must be strictly enforced on both client and server.
     */
    private String buildStringToSign(ServerHttpRequest request, String body) {
        HttpHeaders headers = request.getHeaders();
        String method = request.getMethod().name();
        String host = headers.getFirst(HttpHeaders.HOST);
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery() != null ? URLDecoder.decode(request.getURI().getQuery(), StandardCharsets.UTF_8) : "";
        String timestamp = headers.getFirst(TIMESTAMP_HEADER);
        String nonce = headers.getFirst(NONCE_HEADER);

        // The format is: HTTP_METHOD + "\n" + HOST + "\n" + PATH + "\n" + QUERY_STRING + "\n" + TIMESTAMP + "\n" + NONCE + "\n" + BODY
        return String.join("\n", method, host, path, query, timestamp, nonce, body);
    }

    /**
     * Calculates the HMAC-SHA256 signature for the given data and secret key.
     */
    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error calculating HMAC-SHA256 signature", e);
            // In a real application, you might want a more specific exception.
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    /**
     * A helper method to generate a 401 Unauthorized response.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.warn("Authentication failed: {}", message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        // You could optionally write the message to the response body here.
        return exchange.getResponse().setComplete();
    }
}
