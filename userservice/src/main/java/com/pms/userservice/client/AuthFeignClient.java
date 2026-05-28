package com.pms.userservice.client;

import com.pms.userservice.config.FeignConfig;
import com.pms.userservice.dto.InternalUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(
    name = "auth-service",
    url = "${services.auth.url:http://localhost:8081}",
    configuration = FeignConfig.class
)
public interface AuthFeignClient {

    @GetMapping("/api/v1/internal/users/{id}")
    InternalUserDto getUserInfo(@PathVariable("id") UUID id);

    @PostMapping("/api/v1/internal/users/bulk")
    Map<UUID, String> getBulkUsernames(@RequestBody List<UUID> ids);
}
