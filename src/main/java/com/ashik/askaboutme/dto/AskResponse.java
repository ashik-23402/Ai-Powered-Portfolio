package com.ashik.askaboutme.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AskResponse(
        @Schema(
                description = "Gemini's answer, grounded in the retrieved context. "
                        + "States it doesn't know if no relevant context was found.",
                example = "The AskAboutMe project was created by Ashik. It is a Spring Boot application "
                        + "for retrieval-augmented question answering.")
        String answer
) {
}
