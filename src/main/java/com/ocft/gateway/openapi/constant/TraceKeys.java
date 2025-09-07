package com.ocft.gateway.openapi.constant;

public interface TraceKeys {
    String TRACE_ID_HEADER = "X-Trace-Id";  // 你也可以用 traceparent 等
    String TRACE_ID_CTX_KEY = "traceId";    // Reactor Context key
    String TRACE_ID_MDC_KEY = "traceId";    // MDC key, 供 %X{traceId} 打印
    String TRACE_ID_ATTR = "traceId";       // exchange attribute key
}
