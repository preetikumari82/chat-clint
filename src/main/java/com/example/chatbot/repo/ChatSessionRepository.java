package com.example.chatbot.repo;

import com.example.chatbot.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
}