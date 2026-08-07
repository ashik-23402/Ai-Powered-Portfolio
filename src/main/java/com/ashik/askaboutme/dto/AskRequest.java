package com.ashik.askaboutme.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AskRequest(
        @Schema(
                description = "Natural-language question to answer using the embedded documents",
                example = "What is the AskAboutMe project for?",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "question must not be blank")
        String question
) {
}
