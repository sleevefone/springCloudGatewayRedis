package com.ocft.gateway.openapi.admin;

import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A service to discover and provide a list of available GatewayFilterFactory beans.
 */
@Service
public class GatewayFilterService implements ApplicationContextAware, InitializingBean {
    @Setter
    private ApplicationContext applicationContext;
    private List<String> cachedFilterNames;
    private List<String> cachedPredicateNames;


    /**
     * This method is called once the application context has been initialized.
     * It scans for all GatewayFilterFactory beans and caches their names.
     */
    @Override
    public void afterPropertiesSet() {
        // Get all beans of type GatewayFilterFactory using the correct class
        handleFilter();
        handlePredicate();
    }

    private void handleFilter() {
        String[] beanNames = applicationContext.getBeanNamesForType(GatewayFilterFactory.class);

        this.cachedFilterNames = Arrays.stream(beanNames)
                .map(name -> {
                    String strippedName = name.replace("GatewayFilterFactory", "");
                    // Capitalize the first letter to match the convention (e.g., addTimestamp -> AddTimestamp)
                    if (strippedName.isEmpty()) {
                        return strippedName;
                    }
                    return Character.toUpperCase(strippedName.charAt(0)) + strippedName.substring(1);
                })
                .sorted()
                .collect(Collectors.toList());
    }

    private void handlePredicate() {
        String[] beanNames = applicationContext.getBeanNamesForType(RoutePredicateFactory.class);

        this.cachedPredicateNames = Arrays.stream(beanNames)
                .map(name -> {
                    String strippedName = name.replace("RoutePredicateFactory", "");
                    // Capitalize the first letter to match the convention (e.g., addTimestamp -> AddTimestamp)
                    if (strippedName.isEmpty()) {
                        return strippedName;
                    }
                    return Character.toUpperCase(strippedName.charAt(0)) + strippedName.substring(1);
                })
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Returns the cached list of available gateway filter names.
     *
     * @return A list of filter names.
     */
    public List<String> getAvailableFilters() {
        return Collections.unmodifiableList(this.cachedFilterNames);
    }
    /**
     * Returns the cached list of available gateway filter names.
     *
     * @return A list of filter names.
     */
    public List<String> getAvailablePredicates() {
        return Collections.unmodifiableList(this.cachedPredicateNames);
    }
}
