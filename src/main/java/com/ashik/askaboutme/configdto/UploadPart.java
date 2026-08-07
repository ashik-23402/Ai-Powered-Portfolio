package com.ashik.askaboutme.configdto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UploadPart(
        @Schema(description = "1-based position of this part within the file", example = "1")
        int partNumber,

        @Schema(description = "ETag returned by the storage provider for this part; required by /complete to assemble the file", example = "d41d8cd98f00b204e9800998ecf8427e")
        String etag
) {
}
