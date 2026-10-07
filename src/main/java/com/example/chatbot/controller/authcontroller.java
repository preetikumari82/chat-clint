package com.example.chatbot.controller;

import com.example.chatbot.dto.LoginRequest;
import com.example.chatbot.dto.RefreshRequest;
import com.example.chatbot.dto.TokenResponse;
import com.example.chatbot.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

   
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {

        TokenResponse response = authService.login(req);

        System.out.println("=================================");
        System.out.println("ACCESS TOKEN  : " + response.accessToken());
        System.out.println("REFRESH TOKEN : " + response.refreshToken());
        System.out.println("=================================");

        return response;
    }
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {

        TokenResponse response = authService.refresh(req.refreshToken());

        System.out.println("=================================");
        System.out.println("NEW ACCESS TOKEN  : " + response.accessToken());
        System.out.println("NEW REFRESH TOKEN : " + response.refreshToken());
        System.out.println("=================================");

        return response;
    }
}