package com.ocft.gateway.openapi.admin;

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
import java.util.*;
import java.util.stream.Collectors;

/**
 * A service to discover and provide detailed information about available factories.
 */
@Service
public class GatewayFilterService implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;
    private List<FactoryInfo> cachedFilterFactories;
    private List<FactoryInfo> cachedPredicateFactories;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        this.cachedFilterFactories = scanFactories(GatewayFilterFactory.class, "GatewayFilterFactory");
        this.cachedPredicateFactories = scanFactories(RoutePredicateFactory.class, "RoutePredicateFactory");
    }

    private <T> List<FactoryInfo> scanFactories(Class<T> type, String suffixToRemove) {
        return Arrays.stream(applicationContext.getBeanNamesForType(type))
                .map(beanName -> {
                    Object factory = applicationContext.getBean(beanName);
                    String name = StringUtils.capitalize(beanName.replace(suffixToRemove, ""));
                    List<FactoryArg> args = extractArguments(factory.getClass());
                    return new FactoryInfo(name, factory.getClass().getName(), args);
                })
                .sorted(Comparator.comparing(FactoryInfo::name))
                .collect(Collectors.toList());
    }

    private List<FactoryArg> extractArguments(Class<?> factoryClass) {
        List<FactoryArg> args = new ArrayList<>();
        // Find the config class from the generic type of AbstractGatewayFilterFactory or AbstractRoutePredicateFactory
        Type genericSuperclass = factoryClass.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            Type[] actualTypeArguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
            if (actualTypeArguments.length > 0 && actualTypeArguments[0] instanceof Class) {
                Class<?> configClass = (Class<?>) actualTypeArguments[0];
                // Exclude Object.class which means no specific config
                if (configClass != Object.class) {
                    for (Field field : configClass.getDeclaredFields()) {
                        args.add(new FactoryArg(field.getName(), field.getType().getSimpleName()));
                    }
                }
            }
        }
        return args;
    }

    public List<FactoryInfo> getAvailableFilters() {
        return Collections.unmodifiableList(this.cachedFilterFactories);
    }

    public List<FactoryInfo> getAvailablePredicates() {
        return Collections.unmodifiableList(this.cachedPredicateFactories);
    }

    // DTOs for carrying the factory information
    public record FactoryInfo(String name, String className, List<FactoryArg> args) {}
    public record FactoryArg(String name, String type) {}
}
