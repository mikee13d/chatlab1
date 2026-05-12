package com.example.chatlab.dto;

import java.util.List;

public record LlmResponse(List<Choice> choices) {
    public record Choice(LlmMessage message) {}
}
