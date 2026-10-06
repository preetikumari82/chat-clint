package com.example.chatbot.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "admin_user")
@Data
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    private boolean enabled = true;
}