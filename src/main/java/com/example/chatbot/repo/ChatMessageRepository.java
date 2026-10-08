package com.example.chatbot.repo;

import com.example.chatbot.model.ChatMessage;
import com.example.chatbot.model.MessageRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Memory ke liye: session ke messages purane se naye (session.id par navigate). */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    /** Sirf last N messages chahiye hon to Pageable (desc sort) ke saath use karenge. */
    List<ChatMessage> findBySessionIdOrderByCreatedAtDesc(UUID sessionId, Pageable pageable);

    /** "Unanswered questions" report (FR-SET-04): bot ke fallback replies. */
    Page<ChatMessage> findByRoleAndFallbackTrue(MessageRole role, Pageable pageable);

    long countByRole(MessageRole role);

    long countByCacheHitTrue();

    long countByFallbackTrue();

    /** Retention: purane chats hatane ke liye (FR-CHAT-10). */
    @Modifying
    @Query("delete from ChatMessage m where m.createdAt < :before")
    int deleteOlderThan(Instant before);
}