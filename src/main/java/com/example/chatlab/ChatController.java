package com.example.chatlab;


import com.example.chatlab.dto.ChatRequest;
import com.example.chatlab.dto.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.personality() == null) {
            return ResponseEntity.badRequest().build();
        }

        ChatResponse response = chatService.processChat(request);
        return ResponseEntity.ok(response);
    }
}