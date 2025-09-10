package com.ocft.gateway.openapi.admin;

import lombok.Getter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A service to discover and provide detailed information about available factories.
 * Its single responsibility is to scan and cache factory metadata on startup.
 */
@Service
public class GatewayFilterService implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;
    @Getter
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
                    Class<?> factoryClass = ClassUtils.getUserClass(factory);
                    String name = StringUtils.capitalize(beanName.replace(suffixToRemove, ""));
                    List<FactoryArg> args = extractArguments(factoryClass);
                    return new FactoryInfo(name, factoryClass.getName(), args);
                })
                .sorted(Comparator.comparing(FactoryInfo::name))
                .collect(Collectors.toList());
    }

    private List<FactoryArg> extractArguments(Class<?> factoryClass) {
        List<FactoryArg> args = new ArrayList<>();
        Class<?> currentClass = factoryClass;
        while (currentClass != null && currentClass != Object.class) {
            Type genericSuperclass = currentClass.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
                Type rawType = parameterizedType.getRawType();
                if (rawType.equals(AbstractGatewayFilterFactory.class) || rawType.equals(AbstractRoutePredicateFactory.class)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                        Class<?> configClass = (Class<?>) typeArguments[0];
                        if (configClass != Object.class) {
                            for (Field field : configClass.getDeclaredFields()) {
                                args.add(new FactoryArg(field.getName(), field.getType().getSimpleName()));
                            }
                        }
                    }
                    break;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return args;
    }

    public List<FactoryInfo> getAvailableFilters() {
        return Collections.unmodifiableList(this.cachedFilterFactories);
    }
    public List<String> getAvailableFilterNames() {
        return Collections.unmodifiableList(this.cachedFilterFactories).stream().map(FactoryInfo::name).toList();
    }

    public List<FactoryInfo> getAvailablePredicates() {
        return Collections.unmodifiableList(this.cachedPredicateFactories);
    }
    public List<String> getAvailablePredicateNames() {
        return Collections.unmodifiableList(this.cachedPredicateFactories).stream().map(FactoryInfo::name).toList();
    }

    // DTOs no longer contain source file path, as it's not this service's responsibility.
    public record FactoryInfo(String name, String className, List<FactoryArg> args) {}
    public record FactoryArg(String name, String type) {}
}
