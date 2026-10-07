package com.example.chatbot.service;

import com.example.chatbot.dto.LoginRequest;
import com.example.chatbot.dto.TokenResponse;
import com.example.chatbot.model.AdminUser;
import com.example.chatbot.repo.AdminUserRepository;
import com.example.chatbot.securty.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminUserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public TokenResponse login(LoginRequest req) {
        // Email ya password galat ho, dono mein ek hi message (hacker ko hint nahi)
        AdminUser user = repo.findByEmail(req.email())
                .filter(AdminUser::isEnabled)
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return buildTokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        // Access token ko refresh ke taur par chalne nahi denge
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        AdminUser user = repo.findByEmail(claims.getSubject())
                .filter(AdminUser::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        return buildTokens(user);
    }

    private TokenResponse buildTokens(AdminUser user) {
        return new TokenResponse(
        		jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                jwtService.accessSeconds());
    }
}