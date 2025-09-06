package com.ocft.gateway.openapi.admin;

import lombok.Data;

/**
 * 用于接收 /hello 端点请求体的载荷
 */
@Data
public class HelloRequestPayload {
    private String id;
}