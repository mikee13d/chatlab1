package com.example.chatlab;

import com.example.chatlab.dto.ChatRequest;
import com.example.chatlab.dto.ChatResponse;
import com.example.chatlab.dto.LlmMessage;
import com.example.chatlab.dto.LlmRequest;
import com.example.chatlab.dto.LlmResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private final RestClient restClient;
    // In-memory storage: Maps a Session ID to a list of messages (conversation history)
    private final Map<String, List<LlmMessage>> chatMemory = new ConcurrentHashMap<>();

    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    public ChatService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public ChatResponse processChat(ChatRequest request) {
        // 1. Handle Session ID (Create new if not provided)
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId()
                : UUID.randomUUID().toString();

        // 2. Fetch or create conversation history for this session
        List<LlmMessage> history = chatMemory.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()));

        // 3. If new conversation, inject the personality system prompt
        if (history.isEmpty()) {
            history.add(new LlmMessage("system", getSystemPrompt(request.personality())));
        }

        // 4. Add the user's new message to memory
        history.add(new LlmMessage("user", request.message()));

        // 5. Send request to external LLM (using OpenRouter/OpenAI format)
        LlmRequest llmRequest = new LlmRequest("openai/gpt-3.5-turbo", history); // Example model name

        LlmResponse response = restClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(llmRequest)
                .retrieve()
                .body(LlmResponse.class);

        // 6. Extract AI response, save to memory, and return to user
        String aiText = response.choices().get(0).message().content();
        history.add(new LlmMessage("assistant", aiText));

        return new ChatResponse(aiText, sessionId);
    }

    private String getSystemPrompt(String personality) {
        if (personality == null) return "You are a helpful assistant.";

        return switch (personality.toLowerCase()) {
            case "pirate" -> "You are a salty sea pirate. Talk in pirate slang and mention treasure.";
            case "coder" -> "You are a senior Java developer. Provide code snippets and technical advice.";
            case "poet" -> "You are a romantic poet. Your responses must always be in rhyme.";
            default -> "You are a helpful assistant.";
        };
    }
}