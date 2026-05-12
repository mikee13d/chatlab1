package com.example.chatlab.dto;

public record ChatRequest(String personality, String message, String sessionId) {
}