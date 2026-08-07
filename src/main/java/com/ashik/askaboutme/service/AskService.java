package com.ashik.askaboutme.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Retrieval-augmented answering: pull the most relevant chunks out of pgvector for the question,
 * then let Gemini answer grounded in that context.
 * <p>
 * The context/question are passed to the model as pre-built {@link SystemMessage}/{@link UserMessage}
 * via {@code ChatClient.prompt().messages(...)} rather than the {@code system(String)}/{@code user(String)}
 * convenience overloads - those run the string through ChatClient's ST4 template renderer, which
 * throws if the text contains literal {@code {}} characters (very likely in text extracted from
 * real documents: JSON, code blocks, tables). Passing Message objects directly skips templating entirely.
 */
@Slf4j
@Service
public class AskService {

    private static final int TOP_K = 5;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public AskService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    public String ask(String question) {
        List<Document> matches = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(TOP_K)
                .build());
        log.info("Found {} context chunk(s) for question", matches.size());

        String context = matches.isEmpty()
                ? "(no relevant context was found in the knowledge base)"
                : matches.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));

        SystemMessage systemMessage = new SystemMessage("""
                You are a helpful assistant. Answer the user's question using ONLY the context below,
                which was retrieved from their uploaded documents. If the answer isn't contained in the
                context, say you don't know rather than guessing.

                Context:
                """ + context);

        return chatClient.prompt()
                .messages(systemMessage, new UserMessage(question))
                .call()
                .content();
    }
}
