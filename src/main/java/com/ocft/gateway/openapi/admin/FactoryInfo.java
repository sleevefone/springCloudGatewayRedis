package com.ocft.gateway.openapi.admin;

import lombok.Data;

import java.util.List;

/**
 * A DTO to hold detailed information about a discovered factory, including its parameters.
 */
@Data
public class FactoryInfo {
    private String name;
    private String className;
    private List<ParameterInfo> parameters;

    @Data
    public static class ParameterInfo {
        private String name;
        private String type;

        public ParameterInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
