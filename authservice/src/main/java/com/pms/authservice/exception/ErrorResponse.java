package com.pms.authservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private int status;
    private String message;
    private long timestamp;
    private String path;  

    // Only present for validation errors
    private Map<String, String> errors;
}