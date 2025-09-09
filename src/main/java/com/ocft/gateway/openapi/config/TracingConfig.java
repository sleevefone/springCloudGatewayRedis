//package com.ocft.gateway.openapi.config;
//
//import brave.context.slf4j.MDCScopeDecorator;
//import brave.propagation.CurrentTraceContext;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * Explicit configuration for tracing to ensure MDC propagation.
// * This class manually creates the bean that bridges Brave's trace context with SLF4J's MDC.
// * This is a robust way to ensure tracing works, even if auto-configuration fails for some reason.
// */
//@Configuration
//@ConditionalOnClass(MDCScopeDecorator.class) // Only apply this config if Brave is on the classpath
//public class TracingConfig {
//
//    /**
//     * Creates a bean for the MDCScopeDecorator.
//     * This decorator is the key component that puts the traceId and spanId into the MDC
//     * whenever a new span is created.
//     * By defining it explicitly as a bean, we ensure it's always present in the application context
//     * and correctly integrated into the Micrometer Tracing lifecycle.
//     * @return A new instance of MDCScopeDecorator, configured to be used by the tracer.
//     */
//    @Bean
//    public CurrentTraceContext.ScopeDecorator mdcScopeDecorator() {
//        return MDCScopeDecorator.create();
//    }
//}
