package com.ocft.gateway.openapi.constant;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

import java.util.Objects;
import java.util.function.Function;

import static com.ocft.gateway.openapi.constant.TraceKeys.TRACE_ID_CTX_KEY;
import static com.ocft.gateway.openapi.constant.TraceKeys.TRACE_ID_MDC_KEY;

public final class ReactiveMdcOperator {

    private ReactiveMdcOperator() {}

    public static <T> Function<Publisher<T>, Publisher<T>> lift() {
        Function<? super Publisher<T>, ? extends Publisher<T>> lift = Operators.lift((sc, actual) -> {
            CoreSubscriber<T> coreSubscriber = new CoreSubscriber<>() {

                @Override
                public void onSubscribe(Subscription s) {
                    actual.onSubscribe(s);
                }

                @Override
                public void onNext(T t) {
                    withMdc(() -> actual.onNext(t));
                }

                @Override
                public void onError(Throwable t) {
                    withMdc(() -> actual.onError(t));
                }

                @Override
                public void onComplete() {
                    withMdc(actual::onComplete);
                }

                @Override
                public Context currentContext() {
                    return actual.currentContext();
                }

                private void withMdc(Runnable task) {
                    String traceId = null;
                    try {
                        if (currentContext().hasKey(TRACE_ID_CTX_KEY)) {
                            traceId = Objects.toString(currentContext().get(TRACE_ID_CTX_KEY), null);
                        }
                        if (traceId != null) {
                            MDC.put(TRACE_ID_MDC_KEY, traceId);
                        }
                        task.run();
                    } finally {
                        if (traceId != null) {
                            MDC.remove(TRACE_ID_MDC_KEY);
                        }
                    }
                }
            };
            CoreSubscriber<Object> coreSubscriber1 = (CoreSubscriber<Object>) coreSubscriber;
            return coreSubscriber1;
        });
        return (Function<Publisher<T>, Publisher<T>>) lift;
    }
}
