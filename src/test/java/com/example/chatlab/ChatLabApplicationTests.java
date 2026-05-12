package com.example.chatlab;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.chatlab.dto.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WireMockTest(httpPort = 8081)
class ChatLabApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Point the application to our WireMock server instead of the real API
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("llm.api.url", () -> "http://localhost:8081/chat/completions");
    }

    @Test
    void testChatEndpointWithMockedLLM() throws Exception {
        // 1. Mock the external API response (WireMock)
        String mockApiResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "A poet I am, with words I play."
                      }
                    }
                  ]
                }
                """;

        stubFor(WireMock.post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockApiResponse)));

        // 2. Create the request body for our local endpoint
        ChatRequest request = new ChatRequest("poet", "Who are you?", null);

        // 3. Perform the request and verify
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("A poet I am, with words I play."))
                .andExpect(jsonPath("$.sessionId").exists());
    }
}