package com.pms.projectservice.config;

import org.springframework.stereotype.Component;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {

        log.error(
                "Feign error | Method: {} | Status: {}", 
                methodKey, 
                response.status()
        );

        return switch (response.status()) {

            case 400 -> 
                    new RuntimeException(
                        "Bad request to downstream service"
                    );
            
            case 401 -> 
                    new RuntimeException(
                        "Unauthorized downstream request"
                    );
            
            case 403 -> 
                    new RuntimeException(
                        "Forbidden downstream request"
                    );
            
            case 404 -> 
                    new RuntimeException(
                        "Downstream resource not fount"
                    );
            
            case 500 -> 
                    new RuntimeException(
                        "Downstream service internal error"
                    );

            default -> 
                    defaultErrorDecoder.decode(methodKey, response);
        };
    }
}