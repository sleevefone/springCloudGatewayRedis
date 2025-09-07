//package com.ocft.gateway.openapi.constant.grok;
//
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.GatewayFilter;
//import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferUtils;
//import org.springframework.http.MediaType;
//import org.springframework.http.server.reactive.ServerHttpRequest;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//
///**
// * A custom gateway filter factory for modifying request and response bodies.
// * <p>
// * Functionality: Modifies request/response bodies (e.g., Base64 encoding/decoding, adding timestamp).
// * <p>
// * Naming convention: Class name 'BodyModification' + 'GatewayFilterFactory'.
// * In configuration, use 'BodyModification'.
// */
//@Component
//@Slf4j
//public class BodyModificationGatewayFilterFactory extends AbstractGatewayFilterFactory<BodyModificationGatewayFilterFactory.Config> {
//
//    public BodyModificationGatewayFilterFactory() {
//        super(Config.class);
//    }
//
//    @Override
//    public GatewayFilter apply(Config config) {
//        return (exchange, chain) -> {
//            ServerHttpRequest request = exchange.getRequest();
//            String contentType = request.getHeaders().getContentType() != null ?
//                    request.getHeaders().getContentType().toString() : "";
//
//            // Handle request body modification
//            Mono<Void> modifiedRequest = Mono.just(request)
//                    .flatMap(req -> {
//                        if (contentType.contains(MediaType.APPLICATION_JSON_VALUE) && config.isModifyRequest()) {
//                            return DataBufferUtils.join(req.getBody())
//                                    .map(dataBuffer -> {
//                                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
//                                        dataBuffer.read(bytes);
//                                        DataBufferUtils.release(dataBuffer);
//                                        String body = new String(bytes, StandardCharsets.UTF_8);
//                                        try {
//                                            // Example: Decode Base64 request body
//                                            String decodedBody = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
//                                            ServerHttpRequest modified = req.mutate()
//                                                    .body(Flux.just(exchange.getResponse().bufferFactory().wrap(decodedBody.getBytes(StandardCharsets.UTF_8))))
//                                                    .build();
//                                            return exchange.mutate().request(modified).build();
//                                        } catch (IllegalArgumentException e) {
//                                            log.warn("Invalid Base64 in request body: {}", body);
//                                            return exchange;
//                                        }
//                                    });
//                        }
//                        return Mono.just(exchange);
//                    });
//
//            // Handle response body modification
//            return modifiedRequest.flatMap(modifiedExchange -> {
//                ServerHttpResponse response = modifiedExchange.get();
//                String responseContentType = response.getHeaders().getContentType() != null ?
//                        response.getHeaders().getContentType().toString() : "";
//
//                return response.beforeCommit(() -> {
//                    if (responseContentType.contains(MediaType.APPLICATION_JSON_VALUE) && config.isModifyResponse()) {
//                        return DataBufferUtils.join(modifiedExchange.getResponse().getBody())
//                                .map(dataBuffer -> {
//                                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
//                                    dataBuffer.read(bytes);
//                                    DataBufferUtils.release(dataBuffer);
//                                    String body = new String(bytes, StandardCharsets.UTF_8);
//                                    // Add timestamp to JSON response
//                                    String modifiedBody = body.replace("}", ", \"timestamp\": \"" + System.currentTimeMillis() + "\"}");
//                                    // Example: Encode response to Base64
//                                    String encodedBody = Base64.getEncoder().encodeToString(modifiedBody.getBytes(StandardCharsets.UTF_8));
//                                    DataBuffer buffer = response.bufferFactory().wrap(encodedBody.getBytes(StandardCharsets.UTF_8));
//                                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
//                                    response.getHeaders().setContentLength(encodedBody.length());
//                                    return response.writeWith(Flux.just(buffer));
//                                }).switchIfEmpty(Mono.empty());
//                    }
//                    return Mono.empty();
//                }).then(chain.filter(modifiedExchange));
//            });
//        };
//    }
//
//    /**
//     * Configuration class for receiving parameters from route definitions.
//     */
//    @Data
//    public static class Config {
//        private boolean modifyRequest = true;
//        private boolean modifyResponse = true;
//    }
//}