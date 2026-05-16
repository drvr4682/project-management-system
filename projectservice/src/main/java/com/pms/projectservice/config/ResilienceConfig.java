package com.pms.projectservice.config;

import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ResilienceConfig {
    
    public ResilienceConfig() {

        log.info("Resilience4j configuration initialized");
    }
}