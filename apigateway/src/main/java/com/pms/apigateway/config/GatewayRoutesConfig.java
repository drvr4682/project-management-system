package com.pms.apigateway.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.addRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Slf4j
@Configuration
public class GatewayRoutesConfig {

    @Value("${services.auth.url}")
    private String authServiceUrl;

    @Value("${services.project.url}")
    private String projectServiceUrl;

    // FIX: Added task service URL mapping to match the .env TASK_SERVICE_URL entry
    @Value("${services.task.url:http://localhost:8083}")
    private String taskServiceUrl;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {

        log.info("Initializing API Gateway Routes");

        RouterFunction<ServerResponse> authRoute =
                GatewayRouterFunctions.route("auth-service")
                        .route(
                                path("/api/v1/auth/**"),
                                HandlerFunctions.http(authServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .build();

        RouterFunction<ServerResponse> projectRoute =
                GatewayRouterFunctions.route("project-service")
                        .route(
                                path("/api/v1/projects/**"),
                                HandlerFunctions.http(projectServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .build();

        RouterFunction<ServerResponse> adminRoute =
                GatewayRouterFunctions.route("admin-service")
                        .route(
                                path("/api/v1/admin/**"),
                                HandlerFunctions.http(projectServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .build();

        // FIX: Added task service route to match the TASK_SERVICE_URL in .env
        RouterFunction<ServerResponse> taskRoute =
                GatewayRouterFunctions.route("task-service")
                        .route(
                                path("/api/v1/tasks/**"),
                                HandlerFunctions.http(taskServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .build();

        return authRoute
                .and(projectRoute)
                .and(adminRoute)
                .and(taskRoute);
    }
}
