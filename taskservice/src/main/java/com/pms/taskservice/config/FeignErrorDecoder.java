package com.pms.taskservice.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {

        log.error("Feign error | Method: {} | Status: {}", methodKey, response.status());

        return switch (response.status()) {

            case 400 ->
                    new IllegalArgumentException("Bad request to downstream service");

            case 401, 403 ->
                    new RuntimeException("Downstream auth error: " + response.status());

            case 404 ->
                    new IllegalArgumentException("Resource does not exist in downstream service");

            case 500 ->
                    new RuntimeException("Downstream service returned internal error");

            default ->
                    defaultErrorDecoder.decode(methodKey, response);
        };
    }
}
