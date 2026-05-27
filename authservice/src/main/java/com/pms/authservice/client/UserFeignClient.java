package com.pms.authservice.client;

import com.pms.authservice.config.FeignConfig;
import com.pms.authservice.dto.InternalProfileCreationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "user-service",
    url = "${services.user.url:http://localhost:8084}",
    configuration = FeignConfig.class
)
public interface UserFeignClient {

    @PostMapping("/api/v1/internal/users/profile")
    void createProfile(@RequestBody InternalProfileCreationRequest request);
}
