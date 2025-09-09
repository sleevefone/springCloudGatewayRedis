package com.ocft.gateway.openapi.admin;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * A service to discover and provide lists of available GatewayFilterFactory and RoutePredicateFactory beans.
 */
@Service
public class GatewayFilterService implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;
    private Map<String, String> cachedFilterNames;
    private Map<String, String> cachedPredicateNames;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        // Scan for GatewayFilterFactory beans
        this.cachedFilterNames = Arrays.stream(applicationContext.getBeanNamesForType(GatewayFilterFactory.class))
                .collect(Collectors.toMap(
                        this::formatBeanNameForFilter,
                        name -> applicationContext.getBean(name).getClass().getName(),
                        (name1, name2) -> name1 // In case of duplicates, keep the first one
                ));

        // Scan for RoutePredicateFactory beans
        this.cachedPredicateNames = Arrays.stream(applicationContext.getBeanNamesForType(RoutePredicateFactory.class))
                .collect(Collectors.toMap(
                        this::formatBeanNameForPredicate,
                        name -> applicationContext.getBean(name).getClass().getName(),
                        (name1, name2) -> name1
                ));
    }

    /**
     * Returns the cached map of available gateway filter names to their class names.
     * @return An unmodifiable map of filter names.
     */
    public Map<String, String> getAvailableFilters() {
        return Collections.unmodifiableMap(this.cachedFilterNames);
    }

    /**
     * Returns the cached map of available route predicate names to their class names.
     * @return An unmodifiable map of predicate names.
     */
    public Map<String, String> getAvailablePredicates() {
        return Collections.unmodifiableMap(this.cachedPredicateNames);
    }

    private String formatBeanNameForFilter(String beanName) {
        String strippedName = beanName.replace("GatewayFilterFactory", "");
        return StringUtils.capitalize(strippedName);
    }

    private String formatBeanNameForPredicate(String beanName) {
        String strippedName = beanName.replace("RoutePredicateFactory", "");
        return StringUtils.capitalize(strippedName);
    }
}
