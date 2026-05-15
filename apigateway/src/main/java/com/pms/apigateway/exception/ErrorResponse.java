package com.pms.apigateway.exception;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {

    private int status;
    private String message;
    private long timestamp;
    private String path;
}