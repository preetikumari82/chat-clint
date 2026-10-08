package com.example.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Ek visitor ki ek baatcheet. ID UUID hai (1,2,3 nahi) taaki koi doosre ka session guess na kar sake.
 * IP seedha save nahi karte, sirf hash (privacy, NFR-PRIV-01).
 */
@Entity
@Table(name = "chat_session")
@Getter
@Setter
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant startedAt;

    @Column(length = 64)
    private String ipHash;

    @Column(length = 300)
    private String userAgent;

    @Column(length = 10)
    private String language;
}