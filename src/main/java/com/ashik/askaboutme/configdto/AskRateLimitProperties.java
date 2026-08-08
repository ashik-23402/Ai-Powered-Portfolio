package com.ashik.askaboutme.configdto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rate-limit.ask")
@Validated
public record AskRateLimitProperties(
        @Positive(message = "app.rate-limit.ask.max-requests must be positive")
        int maxRequests,

        @NotNull(message = "app.rate-limit.ask.window is required")
        Duration window
) {
}
