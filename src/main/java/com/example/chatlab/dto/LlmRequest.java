package com.example.chatlab.dto;

import java.util.List;

public record LlmRequest(String model, List<LlmMessage> messages) {
}
