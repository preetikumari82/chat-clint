package com.example.chatbot.config;

import com.example.chatbot.security.JWTAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Poori app ka security rulebook.
 *
 *  PUBLIC   : /api/auth/login, /api/auth/refresh, /api/chat/** (end user), /actuator/health
 *  ADMIN    : /api/admin/**  (role ADMIN ya SUPER_ADMIN)
 *  baaki sab: login zaroori
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // JWT header me jaata hai, cookie me nahi -> CSRF attack ka darr nahi
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())            // CorsConfig wala bean use hoga
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/chat/**").permitAll()          // Step 5+ (rate limit lagega)
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // Login nahi -> 401 JSON
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Login required or token invalid\"}");
                })
                // Login hai par role kam -> 403 JSON
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"You do not have permission\"}");
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JWTAuthFilter @Component hai, to Spring Boot use apne aap servlet filter bhi bana deta hai
     * (matlab filter 2 baar chalta). Yahan use band kar rahe hain; ab sirf Security chain me chalega.
     */
    @Bean
    public FilterRegistrationBean<JWTAuthFilter> jwtFilterRegistration(JWTAuthFilter filter) {
        FilterRegistrationBean<JWTAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}