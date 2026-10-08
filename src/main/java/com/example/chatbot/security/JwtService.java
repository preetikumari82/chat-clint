package com.example.chatbot.security;

import com.example.chatbot.model.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT banana aur padhna. Sirf yahi class tokens ke baare me janti hai.
 *
 * Token ke andar (claims): subject = email, role, type = "access" ya "refresh".
 * "type" isliye taaki refresh token ko access token ki jagah use na kiya ja sake.
 */
@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessMillis;
    private final long refreshMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-minutes}") long accessMinutes,
            @Value("${app.jwt.refresh-days}") long refreshDays) {

        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 ke liye 256 bit (32 bytes) minimum. Kamzor secret = app start hi nahi hoga.
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET kam se kam 32 characters ka hona chahiye (abhi: " + bytes.length + ")");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessMillis = accessMinutes * 60 * 1000;
        this.refreshMillis = refreshDays * 24 * 60 * 60 * 1000;
    }

    public String generateAccessToken(AdminUser user) {
        return build(user, TYPE_ACCESS, accessMillis);
    }

    public String generateRefreshToken(AdminUser user) {
        return build(user, TYPE_REFRESH, refreshMillis);
    }

    public long accessSeconds() {
        return accessMillis / 1000;
    }

    /** Token galat / expire / tampered ho to JwtException phenkta hai. */
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