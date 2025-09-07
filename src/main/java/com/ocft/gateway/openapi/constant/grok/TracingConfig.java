package com.ocft.gateway.openapi.constant.grok;

import io.micrometer.context.ContextRegistry;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
public class TracingConfig implements SmartInitializingSingleton {

    @Bean
    public ContextRegistry contextRegistry() {
        ContextRegistry registry = ContextRegistry.getInstance();
        registry.registerThreadLocalAccessor(new TraceIdThreadLocalAccessor());
        return registry;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Hooks.enableAutomaticContextPropagation();

    }
}