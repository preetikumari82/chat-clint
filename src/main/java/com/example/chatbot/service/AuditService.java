package com.example.chatbot.service;

import com.example.chatbot.model.AuditLog;
import com.example.chatbot.repo.AuditLogRepository;
import com.example.chatbot.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** "Kisne kya kiya" ka record (FR-AUTH-05). Har important admin action ke baad bulao. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repo;

    public void log(String action, String entityType, String entityId, String details) {
        AuditLog a = new AuditLog();
        a.setActorEmail(CurrentUser.email());
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setDetails(details);
        repo.save(a);
    }
}