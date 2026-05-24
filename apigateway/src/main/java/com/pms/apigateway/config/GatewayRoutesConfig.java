package com.pms.apigateway.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
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

    @Value("${services.task.url}")
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
                        .filter(propagateCorrelationId())
                        .build();

        RouterFunction<ServerResponse> projectRoute =
                GatewayRouterFunctions.route("project-service")
                        .route(
                                path("/api/v1/projects/**"),
                                HandlerFunctions.http(projectServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .filter(propagateCorrelationId())
                        .build();

        RouterFunction<ServerResponse> adminRoute =
                GatewayRouterFunctions.route("admin-service")
                        .route(
                                path("/api/v1/admin/**"),
                                HandlerFunctions.http(projectServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .filter(propagateCorrelationId())
                        .build();

        RouterFunction<ServerResponse> taskRoute =
                GatewayRouterFunctions.route("task-service")
                        .route(
                                path("/api/v1/tasks/**"),
                                HandlerFunctions.http(taskServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .filter(propagateCorrelationId())
                        .build();

        return authRoute
                .and(projectRoute)
                .and(adminRoute)
                .and(taskRoute);
    }

    private org.springframework.web.servlet.function.HandlerFilterFunction<ServerResponse, ServerResponse>
            propagateCorrelationId() {

        return (request, next) -> {
            String correlationId = request.headers().firstHeader("X-Correlation-Id");
            if (correlationId != null && !correlationId.isBlank()) {
                ServerRequest mutated = ServerRequest.from(request)
                        .header("X-Correlation-Id", correlationId)
                        .build();
                return next.handle(mutated);
            }
            return next.handle(request);
        };
    }
}