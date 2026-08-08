package com.ashik.askaboutme.controller;

import com.ashik.askaboutme.dto.AskRequest;
import com.ashik.askaboutme.dto.AskResponse;
import com.ashik.askaboutme.exception.ErrorResponse;
import com.ashik.askaboutme.service.AskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ask")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AskController {

    private final AskService askService;

    @Operation(
            summary = "Ask a question about the uploaded documents",
            description = "Embeds the question, runs a top-5 similarity search against pgvector, assembles "
                    + "the matched chunks into a context block, and asks Gemini to answer using only that "
                    + "context. If no relevant chunks are found, or the context doesn't contain the answer, "
                    + "the model is instructed to say it doesn't know rather than guessing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answer generated",
                    content = @Content(schema = @Schema(implementation = AskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (blank/missing question)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests from this IP - rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "The embedding model or Gemini call failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return new AskResponse(askService.ask(request.question()));
    }
}
