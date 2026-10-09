package com.example.chatbot.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Abhi jo admin logged-in hai, uska email. Filter ne SecurityContext me daala hota hai. */
public final class CurrentUser {

    private CurrentUser() {}

    public static String email() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.isAuthenticated()) ? a.getName() : "system";
    }
}