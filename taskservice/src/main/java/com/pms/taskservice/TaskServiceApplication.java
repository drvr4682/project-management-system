package com.pms.taskservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@EnableFeignClients
@EnableSpringDataWebSupport(
        pageSerializationMode =
                EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
@SpringBootApplication(scanBasePackages = {
    "com.pms.taskservice",
    "com.pms.common"
})
public class TaskServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskServiceApplication.class, args);
    }
}
