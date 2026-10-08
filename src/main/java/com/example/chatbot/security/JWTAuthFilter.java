package com.example.chatbot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Har request par chalta hai (darwaze ka guard).
 *
 *  1. Header dekho:  Authorization: Bearer <token>
 *  2. Token verify karo (signature + expiry)
 *  3. Sirf "access" type token maano
 *  4. User DB se load karo; agar enabled hai to "logged in" mark kar do
 *
 * Token galat ho to hum error nahi dete — bas user ko "anonymous" rehne dete hain.
 * Phir SecurityConfig decide karta hai: public URL hai to jaane do, protected hai to 401.
 */
@Component
@RequiredArgsConstructor
public class JWTAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parse(header.substring(7));

                if (JwtService.TYPE_ACCESS.equals(claims.get("type", String.class))) {
                    UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());

                    if (user.isEnabled()) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ignored) {
                // Invalid token -> anonymous rahega -> protected URL par 401 milega
            }
        }

        chain.doFilter(request, response);
    }
}