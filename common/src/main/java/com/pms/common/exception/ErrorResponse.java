package com.pms.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Standard error envelope returned by every PMS microservice.
 *
 * <p>Replaces the four near-identical per-service copies:
 * <ul>
 *   <li>authservice — had {@code @JsonInclude(NON_NULL)} but no {@code @NoArgsConstructor}</li>
 *   <li>projectservice — had all Lombok annotations but no {@code @JsonInclude}</li>
 *   <li>taskservice — identical to projectservice</li>
 *   <li>apigateway — lacked the {@code errors} map</li>
 * </ul>
 *
 * <p>This version:
 * <ul>
 *   <li>Includes {@code @JsonInclude(NON_NULL)} so {@code errors} is omitted
 *       when not applicable.</li>
 *   <li>Has all four Lombok annotations so it is usable as both a builder
 *       target and a Jackson deserialization target.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /** HTTP status code (e.g. 400, 401, 403, 404, 500). */
    private int status;

    /** Human-readable error message. */
    private String message;

    /** Epoch-millis timestamp of when the error occurred. */
    private long timestamp;

    /** Request path that produced the error. */
    private String path;

    /**
     * Field-level validation errors.
     * Only present on 400 validation responses; omitted (null) otherwise.
     */
    private Map<String, String> errors;
}
