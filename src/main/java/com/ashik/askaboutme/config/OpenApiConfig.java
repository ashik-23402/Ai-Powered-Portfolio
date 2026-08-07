package com.ashik.askaboutme.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI askAboutMeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AskAboutMe API")
                        .version("v1")
                        .description("""
                                Upload source documents (PDF, DOCX, DOC, MD, TXT), which are chunked and
                                embedded into a pgvector store by a background job, then ask natural-language
                                questions answered by Gemini grounded in that content.

                                Typical flow: POST /files/initiate, then one or more
                                POST /files/{fileId}/parts/{partNumber}, then POST /files/{fileId}/complete.
                                A completed file is automatically embedded within about a minute; after that,
                                POST /ask can answer questions about its content.
                                """))
                .tags(List.of(
                        new Tag()
                                .name("File Upload")
                                .description("Multipart upload lifecycle for source documents. A file that "
                                        + "reaches status COMPLETED is picked up by a scheduled background job "
                                        + "and embedded into the vector store within about a minute."),
                        new Tag()
                                .name("Ask")
                                .description("Retrieval-augmented question answering: similarity search against "
                                        + "the embedded document chunks in pgvector, then Gemini answers grounded "
                                        + "in the top matches.")
                ));
    }
}
