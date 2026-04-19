package com.pms.authservice.exception;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String message;
    private long timestamp;
    private String path;  

    private Map<String, String> errors;
}
