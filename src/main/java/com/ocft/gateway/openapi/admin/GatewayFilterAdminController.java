package com.ocft.gateway.openapi.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin API to expose the list of available Gateway Filters in the system.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.admin", name = "enabled", havingValue = "true")
@SuppressWarnings("unused")
public class GatewayFilterAdminController {

    private final GatewayFilterService gatewayFilterService;
    private final ApplicationContext applicationContext;

    /**
     * Returns a list of all discovered GatewayFilterFactory names.
     * The names are derived from the bean names, e.g., "AddTimestampGatewayFilterFactory" becomes "AddTimestamp".
     * @return A list of available filter names.
     */
    @GetMapping("/filters")
    public List<String> getAvailableFilters() {
        List<GatewayFilterService.FactoryInfo> cachedFilterFactories = gatewayFilterService.getCachedFilterFactories();
        List<String> list = cachedFilterFactories.stream().map(GatewayFilterService.FactoryInfo::name).toList();
        return list;
    }

    private <T> List<String> scanFactories(Class<T> type, String suffixToRemove) {
        return Arrays.stream(applicationContext.getBeanNamesForType(type))
                .map(beanName -> {
                    Object factory = applicationContext.getBean(beanName);
                    Class<?> factoryClass = ClassUtils.getUserClass(factory);
                    String name = StringUtils.capitalize(beanName.replace(suffixToRemove, ""));
                    List<GatewayFilterService.FactoryArg> args = extractArguments(factoryClass);
                    return new GatewayFilterService.FactoryInfo(name, factoryClass.getName(), args);
                })
                .sorted(Comparator.comparing(GatewayFilterService.FactoryInfo::name))
                .map(GatewayFilterService.FactoryInfo::name)
                .collect(Collectors.toList());
    }

    private List<GatewayFilterService.FactoryArg> extractArguments(Class<?> factoryClass) {
        List<GatewayFilterService.FactoryArg> args = new ArrayList<>();
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
                                args.add(new GatewayFilterService.FactoryArg(field.getName(), field.getType().getSimpleName()));
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
}
