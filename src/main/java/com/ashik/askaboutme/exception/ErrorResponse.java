package com.ashik.askaboutme.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Standard error body returned for every failed request")
public record ErrorResponse(
        @Schema(description = "When the error occurred", example = "2026-08-07T13:39:37.069295825Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP status reason phrase", example = "Bad Request")
        String error,

        @Schema(description = "Human-readable error detail", example = "question: question must not be blank")
        String message,

        @Schema(description = "Request path that triggered the error", example = "/api/v1/ask")
        String path
) {
}
