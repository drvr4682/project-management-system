package com.pms.taskservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ResilienceConfig {

    public ResilienceConfig() {
        log.info("Resilience4j configuration initialized");
    }
}
