package com.ocft.gateway.openapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author Shaper
 * 2025-09-06 13:19:24
 */
@SpringBootApplication
@Slf4j
public class OpenapiApplication implements ApplicationListener<ApplicationStartedEvent> {


    public static void main(String[] args) {
        SpringApplication.run(OpenapiApplication.class, args);
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        // 打印服务启动成功的标记
        log.info("[OpenapiApplication started successfully at {}]", event.getTimestamp());
    }
}