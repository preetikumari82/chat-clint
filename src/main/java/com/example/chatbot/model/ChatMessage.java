package com.example.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Chat ka ek message (user ka ya bot ka). Bot ke message par tokens, cost, latency, cache hit,
 * sources aur feedback bhi store hote hain (FR-CHAT-10).
 */
@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_msg_session", columnList = "session_id"),
        @Index(name = "idx_msg_created", columnList = "created_at"),
        @Index(name = "idx_msg_fallback", columnList = "fallback")
})
@Getter
@Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Sources ka JSON text, jaise [{"doc":"policy.pdf","page":2}] */
    @Column(columnDefinition = "TEXT")
    private String sources;

    private Integer tokensIn;
    private Integer tokensOut;

    @Column(precision = 12, scale = 6)
    private BigDecimal cost;

    private Integer latencyMs;

    private boolean cacheHit;

    /** true = relevant chunk nahi mila, fallback reply diya. "Unanswered questions" report isi se banegi (FR-SET-04). */
    private boolean fallback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Feedback feedback = Feedback.NONE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}