package com.pms.taskservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditLogger {

    public void log(String user, String action, Long taskId, String detail) {
        log.info(
                "USER: {} | ACTION: {} | TASK: {} | DETAIL: {}",
                user, action, taskId, detail
        );
    }
}
