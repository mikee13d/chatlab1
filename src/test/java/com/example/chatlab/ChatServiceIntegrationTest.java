package com.example.chatlab;

import com.example.chatlab.dto.ChatRequest;
import com.example.chatlab.dto.ChatResponse;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@SpringBootTest
class ChatServiceIntegrationTest {

    // Start a real WireMock server on a random port for each test class
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    // Point the service at our WireMock server instead of the real API
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("llm.api.url", wireMock::baseUrl);
        registry.add("llm.api.key", () -> "test-key");
    }

    @Autowired
    private ChatService chatService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a minimal valid OpenAI-format JSON response body.
     */
    private String llmResponseBody(String content) {
        return """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "%s"
                      }
                    }
                  ]
                }
                """.formatted(content);
    }

    private void stubLlmOk(String responseText) {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(llmResponseBody(responseText))));
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void processChat_returnsAiResponse_andEchoesSessionId() {
        stubLlmOk("Here is a for-loop example.");

        ChatRequest request = new ChatRequest("coder", "Show me a for-loop", "session-abc");
        ChatResponse response = chatService.processChat(request);

        assertThat(response.response()).isEqualTo("Here is a for-loop example.");
        assertThat(response.sessionId()).isEqualTo("session-abc");
    }

    @Test
    void processChat_generatesSessionId_whenNoneProvided() {
        stubLlmOk("Arr, here be treasure!");

        ChatRequest request = new ChatRequest("pirate", "Where is the treasure?", null);
        ChatResponse response = chatService.processChat(request);

        // Session ID must be auto-generated (non-blank UUID)
        assertThat(response.sessionId()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // Memory / context
    // -------------------------------------------------------------------------

    @Test
    void processChat_sendsConversationHistory_onFollowUpMessages() {
        stubLlmOk("Yes, polymorphism means many forms.");

        ChatRequest first  = new ChatRequest("coder", "What is polymorphism?", "session-mem");
        ChatRequest second = new ChatRequest("coder", "Can you give an example?", "session-mem");

        chatService.processChat(first);

        // Reset stub so second call also succeeds
        wireMock.resetAll();
        stubLlmOk("Sure! Here is an example...");

        chatService.processChat(second);

        // Verify that the second request body contained prior history
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(containing("What is polymorphism?")));
    }

    @Test
    void processChat_isolatesHistory_betweenDifferentSessions() {
        stubLlmOk("Pirate answer.");
        chatService.processChat(new ChatRequest("pirate", "Ahoy!", "session-pirate"));

        wireMock.resetAll();
        stubLlmOk("Coder answer.");
        chatService.processChat(new ChatRequest("coder", "Show me code.", "session-coder"));

        // The second call must NOT contain the first session's message
        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(notContaining("Ahoy!")));
    }

    // -------------------------------------------------------------------------
    // Personality / system prompt
    // -------------------------------------------------------------------------

    @Test
    void processChat_injectsPirateSystemPrompt_whenPersonalityIsPirate() {
        stubLlmOk("Arr!");

        chatService.processChat(new ChatRequest("pirate", "Hello", "session-pirate-2"));

        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(containing("pirate")));
    }

    @Test
    void processChat_usesDefaultPrompt_whenPersonalityIsUnknown() {
        stubLlmOk("I am a helpful assistant.");

        chatService.processChat(new ChatRequest("unknown_personality", "Hello", "session-default"));

        wireMock.verify(postRequestedFor(urlEqualTo("/"))
                .withRequestBody(containing("helpful assistant")));
    }

    // -------------------------------------------------------------------------
    // Error handling — demonstrates WireMock's value for fault injection
    // -------------------------------------------------------------------------

    @Test
    void processChat_throwsException_whenLlmReturns500() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() ->
                chatService.processChat(new ChatRequest("coder", "Hello", "session-err")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void processChat_throwsIllegalStateException_whenChoicesAreEmpty() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "choices": [] }
                                """)));

        assertThatThrownBy(() ->
                chatService.processChat(new ChatRequest("coder", "Hello", "session-empty")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Empty or null response");
    }
}
