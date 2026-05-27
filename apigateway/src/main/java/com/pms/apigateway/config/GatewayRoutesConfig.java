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

    @Value("${services.user.url}")
    private String userServiceUrl;

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
                        .filter(injectAuthHeaders())
                        .build();

        RouterFunction<ServerResponse> projectRoute =
                GatewayRouterFunctions.route("project-service")
                        .route(
                                path("/api/v1/projects/**"),
                                HandlerFunctions.http(projectServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .filter(injectAuthHeaders())
                        .build();

        RouterFunction<ServerResponse> adminRoute =
                GatewayRouterFunctions.route("admin-service")
                        .route(
                                path("/api/v1/admin/**"),
                                HandlerFunctions.http(projectServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .filter(injectAuthHeaders())
                        .build();

        RouterFunction<ServerResponse> taskRoute =
                GatewayRouterFunctions.route("task-service")
                        .route(
                                path("/api/v1/tasks/**"),
                                HandlerFunctions.http(taskServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .filter(injectAuthHeaders())
                        .build();

        RouterFunction<ServerResponse> userRoute =
                GatewayRouterFunctions.route("user-service")
                        .route(
                                path("/api/v1/users/**"),
                                HandlerFunctions.http(userServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .filter(injectAuthHeaders())
                        .build();

        RouterFunction<ServerResponse> socialLinksRoute =
                GatewayRouterFunctions.route("social-links-service")
                        .route(
                                path("/api/v1/social-links/**"),
                                HandlerFunctions.http(userServiceUrl)
                        )
                        .before(addRequestHeader("X-Gateway", "API-GATEWAY"))
                        .before(addRequestHeader("X-Gateway-Secret", gatewaySecret))
                        .filter(injectAuthHeaders())
                        .build();

        return authRoute
                .and(projectRoute)
                .and(adminRoute)
                .and(taskRoute)
                .and(userRoute)
                .and(socialLinksRoute);
    }

    private org.springframework.web.servlet.function.HandlerFilterFunction<ServerResponse, ServerResponse>
            injectAuthHeaders() {

        return (request, next) -> {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            ServerRequest.Builder builder = ServerRequest.from(request);

            if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                String userId = auth.getName();
                String role = auth.getAuthorities().stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                        .map(r -> r.replace("ROLE_", ""))
                        .findFirst()
                        .orElse("USER");

                builder.header("X-User-Id", userId);
                builder.header("X-User-Role", role);
                log.debug("Injected downstream headers: X-User-Id={}, X-User-Role={}", userId, role);
            }

            String correlationId = request.headers().firstHeader("X-Correlation-Id");
            if (correlationId != null && !correlationId.isBlank()) {
                builder.header("X-Correlation-Id", correlationId);
            }

            return next.handle(builder.build());
        };
    }
}