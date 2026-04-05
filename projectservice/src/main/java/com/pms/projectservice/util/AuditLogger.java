package com.pms.projectservice.util;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditLogger {

    public void log(String user, String action, Long projectId, String target) {
        log.info("USER: {} | ACTION: {} | PROJECT: {} | TARGET: {}",
                user,
                action,
                projectId,
                target
        );
    }
}