package com.ocft.gateway.openapi.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A service to discover and provide detailed lists of available factories, including their parameters.
 */
@Service
@Slf4j
public class GatewayFilterService implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;
    private Map<String, FactoryInfo> cachedFilterFactories;
    private Map<String, FactoryInfo> cachedPredicateFactories;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        this.cachedFilterFactories = scanFactories(GatewayFilterFactory.class, "GatewayFilterFactory");
        this.cachedPredicateFactories = scanFactories(RoutePredicateFactory.class, "RoutePredicateFactory");
    }

    private <T> Map<String, FactoryInfo> scanFactories(Class<T> type, String suffixToRemove) {
        return Arrays.stream(applicationContext.getBeanNamesForType(type))
                .map(beanName -> {
                    Object factoryBean = applicationContext.getBean(beanName);
                    return createFactoryInfo(beanName, factoryBean, suffixToRemove);
                })
                .collect(Collectors.toMap(FactoryInfo::getName, info -> info, (info1, info2) -> info1));
    }

    private FactoryInfo createFactoryInfo(String beanName, Object factoryBean, String suffixToRemove) {
        FactoryInfo info = new FactoryInfo();
        String name = StringUtils.capitalize(beanName.replace(suffixToRemove, ""));
        info.setName(name);
        info.setClassName(factoryBean.getClass().getName());

        try {
            // Most factories define their parameters in a nested static class called 'Config'.
            Class<?> configClass = Class.forName(factoryBean.getClass().getName() + "$Config");
            List<FactoryInfo.ParameterInfo> params = Arrays.stream(configClass.getDeclaredFields())
                    .map(field -> new FactoryInfo.ParameterInfo(field.getName(), getFieldType(field)))
                    .collect(Collectors.toList());
            info.setParameters(params);
        } catch (ClassNotFoundException e) {
            // This factory does not have a 'Config' class, likely takes no parameters.
            log.debug("No 'Config' class found for factory bean: {}. Assuming no parameters.", beanName);
            info.setParameters(Collections.emptyList());
        }
        return info;
    }

    private String getFieldType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            // For generic types like List<String>, show the full type.
            return genericType.getTypeName();
        }
        return field.getType().getSimpleName();
    }

    public Map<String, FactoryInfo> getAvailableFilters() {
        return Collections.unmodifiableMap(this.cachedFilterFactories);
    }

    public Map<String, FactoryInfo> getAvailablePredicates() {
        return Collections.unmodifiableMap(this.cachedPredicateFactories);
    }
}
