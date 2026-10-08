package com.example.chatbot.repo;

import com.example.chatbot.model.BotSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotSettingsRepository extends JpaRepository<BotSettings, Long> {
}