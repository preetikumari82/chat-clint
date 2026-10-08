package com.example.chatbot.security;

import com.example.chatbot.repo.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security ko bata-ta hai ki "email se user kaise dhundna hai".
 * DB ka AdminUser -> Spring ka UserDetails me convert karta hai.
 */
@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var admin = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.withUsername(admin.getEmail())
                .password(admin.getPasswordHash())
                // Spring "ROLE_" prefix expect karta hai: ROLE_ADMIN, ROLE_SUPER_ADMIN
                .authorities("ROLE_" + admin.getRole())
                .disabled(!admin.isEnabled())
                .build();
    }
}