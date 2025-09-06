package com.ocft.gateway.openapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Global CORS Configuration for the application.
 * This is necessary for front-end development with a separate dev server (e.g., Vite, Vue CLI).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow requests from any origin. For production, you might want to restrict this to your actual front-end domain.
        config.addAllowedOrigin("*"); 
        // Allow all standard HTTP methods.
        config.addAllowedMethod("*");
        // Allow all headers.
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Apply this configuration to all paths

        return new CorsWebFilter(source);
    }
}
