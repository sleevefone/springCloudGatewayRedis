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
import java.net.URL;
import java.nio.file.Paths;
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
                    Class<?> factoryClass = factory.getClass();
                    String name = StringUtils.capitalize(beanName.replace(suffixToRemove, ""));
                    List<FactoryArg> args = extractArguments(factoryClass);
                    String sourceFile = findSourceFile(factoryClass);
                    return new FactoryInfo(name, factoryClass.getName(), args, sourceFile);
                })
                .sorted(Comparator.comparing(FactoryInfo::name))
                .collect(Collectors.toList());
    }
    private List<FactoryArg> extractArguments(Class<?> factoryClass) {
        List<FactoryArg> args = new ArrayList<>();
        Type genericSuperclass = factoryClass.getGenericSuperclass();
        while (genericSuperclass != null) {
            if (genericSuperclass instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
                if (actualTypeArguments.length > 0) {
                    Type argType = actualTypeArguments[0];
                    if (argType instanceof Class<?>) {
                        Class<?> configClass = (Class<?>) argType;
                        if (configClass != Object.class) {
                            extractFieldsFromClass(configClass, args);
                            break;
                        }
                    } else if (argType instanceof ParameterizedType) {
                        // 处理嵌套泛型，如 List<String>，提取 rawType 的 fields（如果 rawType 是类）
                        ParameterizedType paramType = (ParameterizedType) argType;
                        Type rawType = paramType.getRawType();
                        if (rawType instanceof Class<?>) {
                            Class<?> configClass = (Class<?>) rawType;
                            if (configClass != Object.class) {
                                extractFieldsFromClass(configClass, args);
                                break;
                            }
                        }
                    } else {
                        // 其他类型（如 TypeVariable），忽略或记录为未知
                        // 可以添加日志：log.warn("Unsupported type: " + argType);
                    }
                }
            }
            // 修复：正确更新 factoryClass 和 genericSuperclass
            Class<?> currentClass = (Class<?>) factoryClass.getSuperclass();
            if (currentClass != null) {
                genericSuperclass = currentClass.getGenericSuperclass();
                factoryClass = currentClass;  // 更新 factoryClass 为当前 superclass
            } else {
                genericSuperclass = null;
            }
        }
        return args;
    }

    // 辅助方法：从 Config 类提取字段
    private void extractFieldsFromClass(Class<?> configClass, List<FactoryArg> args) {
        for (Field field : configClass.getDeclaredFields()) {
            // 获取字段的简单类型名（处理复杂泛型）
            Type fieldType = field.getGenericType();
            String typeName = getTypeSimpleName(fieldType);
            args.add(new FactoryArg(field.getName(), typeName));
        }
    }

    // 辅助方法：获取 Type 的简单名称（处理 ParameterizedType）
    private String getTypeSimpleName(Type type) {
        if (type instanceof Class<?>) {
            return ((Class<?>) type).getSimpleName();
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type rawType = paramType.getRawType();
            if (rawType instanceof Class<?>) {
                return ((Class<?>) rawType).getSimpleName() + "<...>";  // 简化表示泛型，如 List<...>
            }
        }
        //  fallback
        return type.toString();
    }

    private String findSourceFile(Class<?> clazz) {
        try {
            String className = clazz.getName();
            String classAsPath = className.replace('.', '/') + ".class";
            URL url = clazz.getClassLoader().getResource(classAsPath);
            if (url != null && "file".equals(url.getProtocol())) {
                String path = url.getPath();
                // This logic assumes a standard Maven/Gradle project structure
                String targetPath = "/target/classes/";
                String buildPath = "/build/classes/java/main/";
                String sourcePath = "/src/main/java/";
                String sourceFile = className.replace('.', '/') + ".java";

                if (path.contains(targetPath)) {
                    return Paths.get(path.substring(0, path.indexOf(targetPath)), sourcePath, sourceFile).toString();
                } else if (path.contains(buildPath)) {
                    return Paths.get(path.substring(0, path.indexOf(buildPath)), sourcePath, sourceFile).toString();
                }
            }
        } catch (Exception e) {
            // Ignore, as this is a best-effort enhancement
        }
        return null;
    }

    public List<FactoryInfo> getAvailableFilters() {
        return Collections.unmodifiableList(this.cachedFilterFactories);
    }

    public List<FactoryInfo> getAvailablePredicates() {
        return Collections.unmodifiableList(this.cachedPredicateFactories);
    }

    // DTOs now include the source file path
    public record FactoryInfo(String name, String className, List<FactoryArg> args, String sourceFile) {}
    public record FactoryArg(String name, String type) {}
}
