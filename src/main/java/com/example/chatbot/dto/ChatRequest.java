package com.example.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/chat ka body. Max 1000 chars (SRS A.1 max-question-chars, FR-CHAT-08). */
public record ChatRequest(
        @NotBlank @Size(max = 1000) String question,
        String sessionId          // pehli baar null; baad me server wala id wapas bhejna
) {}