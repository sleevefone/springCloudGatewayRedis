package com.ocft.gateway.openapi.admin;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A DTO for managing filters in the admin UI, including an 'enabled' flag.
 */
@Data
public class FilterInfo {

    private String name;

    private Map<String, String> args = new LinkedHashMap<>();

}
