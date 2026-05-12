package com.example.chatlab;

import com.example.chatlab.dto.ChatRequest;
import com.example.chatlab.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;  // Boot 4.x package
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void chat_returnsOkWithResponse_whenRequestIsValid() throws Exception {
        ChatResponse stubResponse = new ChatResponse("Use a for-loop like this: ...", "session-1");
        when(chatService.processChat(any(ChatRequest.class))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personality": "coder",
                                  "message": "How do I write a for-loop?",
                                  "sessionId": "session-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("Use a for-loop like this: ..."))
                .andExpect(jsonPath("$.sessionId").value("session-1"));
    }

    @Test
    void chat_generatesNewSessionId_whenSessionIdIsOmitted() throws Exception {
        ChatResponse stubResponse = new ChatResponse("Arr, the treasure be buried!", "generated-uuid");
        when(chatService.processChat(any(ChatRequest.class))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personality": "pirate",
                                  "message": "Where is the treasure?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // Validation — 400 when required fields are missing
    // -------------------------------------------------------------------------

    @Test
    void chat_returns400_whenMessageIsNull() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personality": "coder",
                                  "message": null,
                                  "sessionId": "session-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_returns400_whenPersonalityIsNull() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personality": null,
                                  "message": "Hello",
                                  "sessionId": "session-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}