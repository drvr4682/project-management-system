package com.pms.userservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditLogger {

    public void log(String user, String action, String profileId, String detail) {
        log.info("USER: {} | ACTION: {} | PROFILE: {} | DETAIL: {}",
                user,
                action,
                profileId,
                detail
        );
    }
}
