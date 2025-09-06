package com.ocft.gateway.openapi.unit;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class TenantUnitMappingService {
    public String getUnitByTenantId(String tenantId) {
        return tenantId;
    }
}
