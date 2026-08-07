package com.ashik.askaboutme.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class DocumentExtractor {

    public List<Document> extract(Resource resource, String fileName) {
        DocumentReader reader = switch (extensionOf(fileName)) {
            case "pdf" -> new PagePdfDocumentReader(resource,
                    PdfDocumentReaderConfig.builder()
                            .withPagesPerDocument(1)
                            .build());
            case "docx", "doc" -> new TikaDocumentReader(resource);
            case "md" -> new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.defaultConfig());
            case "txt" -> new TextReader(resource);
            default -> throw new IllegalArgumentException("Unsupported file type for: " + fileName);
        };
        return reader.get();
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("File name is required to determine document type");
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("Cannot determine file extension for: " + fileName);
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
