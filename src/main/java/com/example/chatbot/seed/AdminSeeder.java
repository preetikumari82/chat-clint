package com.example.chatbot.seed;

import com.example.chatbot.model.AdminUser;
import com.example.chatbot.repo.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final AdminUserRepository repo;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Override
    public void run(String... args) {
        if (repo.existsByEmail(email)) {
            log.info("Admin already exists: {}", email);
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setEmail(email);
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole("SUPER_ADMIN");
        admin.setEnabled(true);
        repo.save(admin);
        log.info("Admin created: {}", email);
    }
}