package com.example.chatbot.securty;

import com.example.chatbot.model.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessMillis;
    private final long refreshMillis;

    // Dhyan dein: yahan "void" nahi hai. Ye asli constructor hai.
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-minutes}") long accessMinutes,
            @Value("${app.jwt.refresh-days}") long refreshDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMillis = accessMinutes * 60 * 1000;
        this.refreshMillis = refreshDays * 24 * 60 * 60 * 1000;
    }

    public String generateAccessToken(AdminUser user) {
        return build(user, "access", accessMillis);
    }

    public String generateRefreshToken(AdminUser user) {
        return build(user, "refresh", refreshMillis);
    }

    public long accessSeconds() {
        return accessMillis / 1000;
    }

    // Token galat ya expire ho to exception phenkta hai
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String build(AdminUser user, String type, long validMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole())
                .claim("type", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + validMillis))
                .signWith(key)
                .compact();
    }
}