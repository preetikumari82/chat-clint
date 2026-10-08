package com.example.chatbot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Bot ki settings. Phase 1 me sirf ek row (id = 1).
 * Phase 2 me har bot ki apni row hogi.
 */
@Entity
@Table(name = "bot_settings")
@Getter
@Setter
public class BotSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name = "Company Assistant";

    @Column(length = 500)
    private String welcomeMessage = "Hi! Ask me anything about our documents.";

    @Column(length = 500)
    private String fallbackMessage =
            "I could not find this in the available documents. Please try rephrasing your question.";

    /** Bot ke niyam (SRS A.4). Context aur question code me jodte hain, yahan nahi. */
    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    /** Model ka naam config se aata hai, code me hard-code nahi (NFR-MAINT-01). */
    @Column(nullable = false, length = 100)
    private String chatModel;

    /** Sirf padhne ke liye. Badalne par SAARE documents dobara embed karne padte hain. */
    @Column(nullable = false, length = 100)
    private String embedModel;

    private int topK = 5;
    private double similarityThreshold = 0.60;
    private double temperature = 0.2;
    private int maxTokens = 500;

    @UpdateTimestamp
    private Instant updatedAt;
}