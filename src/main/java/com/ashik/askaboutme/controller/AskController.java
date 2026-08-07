package com.ashik.askaboutme.controller;

import com.ashik.askaboutme.dto.AskRequest;
import com.ashik.askaboutme.dto.AskResponse;
import com.ashik.askaboutme.service.AskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AskController {

    private final AskService askService;

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return new AskResponse(askService.ask(request.question()));
    }
}
