package com.ocft.gateway.openapi.admin;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of the SourceCodeService.
 */
@Service
public class SourceCodeServiceImpl implements SourceCodeService {

    @Override
    public String getSourceCode(String className) throws IOException {
        // Use a resource resolver to find the source file in the classpath, including inside JARs.
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String resourcePath = "classpath*:/" + className.replace('.', '/') + ".java";
        
        Resource[] resources = resolver.getResources(resourcePath);
        
        if (resources.length > 0) {
            // If found, read the content of the first match.
            try (InputStream inputStream = resources[0].getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } else {
            // If no source file is found, return a clear message.
            return String.format("Source code for class '%s' not found in classpath.", className);
        }
    }
}
