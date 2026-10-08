package com.example.chatbot.controller;

import com.example.chatbot.dto.LoginRequest;
import com.example.chatbot.dto.RefreshRequest;
import com.example.chatbot.dto.TokenResponse;
import com.example.chatbot.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        // Tokens kabhi log/print mat karo.
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    /** Test endpoint: token sahi hai to batata hai "main kaun hoon". Frontend bhi isse use karega. */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")).toList();
        return Map.of("email", auth.getName(), "roles", roles);
    }
}