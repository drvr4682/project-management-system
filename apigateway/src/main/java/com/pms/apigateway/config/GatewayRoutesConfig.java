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

    @Bean
    public RouterFunction<ServerResponse> gatewayRoutes() {

        log.info("Initializing API Gateway Routes");

        RouterFunction<ServerResponse> authRoute =
                GatewayRouterFunctions.route("auth-service")
                        .route(
                                path("/api/v1/auth/**"),
                                HandlerFunctions.http(authServiceUrl)
                        )
                        .before(
                                addRequestHeader(
                                        "X-Gateway",
                                        "API-GATEWAY"
                                )
                        )
                        .build();

        RouterFunction<ServerResponse> projectRoute =
                GatewayRouterFunctions.route("project-service")
                        .route(
                                path("/api/v1/projects/**"),
                                HandlerFunctions.http(projectServiceUrl)
                        )
                        .before(
                                addRequestHeader(
                                        "X-Gateway",
                                        "API-GATEWAY"
                                )
                        )
                        .build();

        return authRoute.and(projectRoute);
    }
}