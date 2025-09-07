package com.ocft.gateway.openapi.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocft.gateway.openapi.admin.ApiClient;
import com.ocft.gateway.openapi.admin.ApiClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class SignatureAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final ApiClientService apiClientService;
    private final ObjectMapper objectMapper;

    public SignatureAuthenticationGatewayFilterFactory(ApiClientService apiClientService, ObjectMapper objectMapper) {
        super(Object.class);
        this.apiClientService = apiClientService;
        this.objectMapper = objectMapper;
    }

    private static final String APP_KEY_HEADER = "X-AppKey";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final String NONCE_HEADER = "X-Nonce";
    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final long TIMESTAMP_VALIDITY_SECONDS = 300; // 5 minutes

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            HttpHeaders headers = request.getHeaders();

            String appKey = headers.getFirst(APP_KEY_HEADER);
            String timestampStr = headers.getFirst(TIMESTAMP_HEADER);
            String nonce = headers.getFirst(NONCE_HEADER);
            String clientSignature = headers.getFirst(SIGNATURE_HEADER);

            if (appKey == null || timestampStr == null || nonce == null || clientSignature == null) {
                return unauthorized(exchange, "Missing required authentication headers.");
            }

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

            return apiClientService.getByAppKey(appKey)
                    .map(apiClient -> {
                        if (!apiClient.isEnabled()) {
                            return unauthorized(exchange, "API client is disabled.");
                        }
                        return verifySignature(exchange, apiClient, clientSignature,chain);
                    })
                    .orElse(unauthorized(exchange, "Invalid AppKey."));
        };
    }

    private Mono<Void> verifySignature(ServerWebExchange exchange, ApiClient client, String clientSignature, GatewayFilterChain chain) {
        return ServerWebExchangeUtils.cacheRequestBody(exchange, serverHttpRequest -> {
            String stringToSign = buildStringToSign(serverHttpRequest, exchange);
            log.debug("Server-side string to sign: \n{}", stringToSign);

            String serverSignature = calculateHmacSha256(stringToSign, client.getSecretKey());
            log.debug("Server signature: {}, Client signature: {}", serverSignature, clientSignature);

            if (Objects.equals(serverSignature, clientSignature)) {
                return chain.filter(exchange.mutate().request(serverHttpRequest).build());
            } else {
                return unauthorized(exchange, "Signature mismatch.");
            }
        });
    }

    private String buildStringToSign(ServerHttpRequest request, ServerWebExchange exchange) {
        HttpHeaders headers = request.getHeaders();
        String method = request.getMethod().name();
        String host = headers.getFirst(HttpHeaders.HOST);
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery() != null ? URLDecoder.decode(request.getURI().getQuery(), StandardCharsets.UTF_8) : "";
        String timestamp = headers.getFirst(TIMESTAMP_HEADER);
        String nonce = headers.getFirst(NONCE_HEADER);
        String body = exchange.getAttributeOrDefault(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR, "");

        return String.join("\n", method, host, path, query, timestamp, nonce, body);
    }

    private String calculateHmacSha256(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error calculating HMAC-SHA256 signature", e);
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    /**
     * **BEST PRACTICE FIX:**
     * A helper method to generate a 401 Unauthorized response with a proper JSON body.
     * It no longer uses setComplete(), which would close the stream prematurely.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.warn("Authentication failed: {}", message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Create a JSON error response body
        Map<String, Object> errorResponse = Map.of(
                "timestamp", System.currentTimeMillis(),
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Unauthorized",
                "message", message,
                "path", exchange.getRequest().getPath().value()
        );

        try {
            byte[] responseBody = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseBody);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing unauthorized response body", e);
            // Fallback to a simple response if JSON processing fails
            return response.setComplete();
        }
    }
}
