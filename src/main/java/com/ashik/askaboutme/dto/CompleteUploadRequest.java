package com.ashik.askaboutme.dto;

import com.ashik.askaboutme.configdto.UploadPart;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompleteUploadRequest(
        @NotEmpty(message = "parts is required") List<UploadPart> parts
) {
}
