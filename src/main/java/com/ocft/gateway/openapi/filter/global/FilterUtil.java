package com.ocft.gateway.openapi.filter.global;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetSocketAddress;
import java.util.List;

import static com.ocft.gateway.openapi.filter.global.RequestLoggingGlobalFilter.X_REAL_IP;
import static org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver.X_FORWARDED_FOR;

public class FilterUtil {

    public static String getRealIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();

        // 1. 检查 X-Forwarded-For
        List<String> forwardedFor = headers.get(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            // 取第一个 IP（客户端真实 IP）
            String ip = forwardedFor.get(0).split(",")[0].trim();
            if (!ip.isEmpty()) {
                return ip;
            }
        }

        // 2. 检查 X-Real-IP
        String realIp = headers.getFirst(X_REAL_IP);
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }

        // 3. 兜底：使用 remoteAddress
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
