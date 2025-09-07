package com.ocft.gateway.openapi.constant.grok;

import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;

public class TraceIdThreadLocalAccessor implements ThreadLocalAccessor<String> {

    public static final String TRACE_ID_KEY = "traceId";

    @Override
    public String key() {
        return TRACE_ID_KEY;
    }

    @Override
    public String getValue() {
        return MDC.get(TRACE_ID_KEY);
    }

    @Override
    public void setValue(String value) {
        if (value != null) {
            MDC.put(TRACE_ID_KEY, value);
        } else {
            MDC.remove(TRACE_ID_KEY); // 清除 MDC
        }
    }

    @Override
    public void reset() {
        // 留空或抛出异常，避免直接操作 MDC
        // Micrometer 会在需要时调用 setValue(null)
    }
}