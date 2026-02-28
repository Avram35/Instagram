package com.instagram.post_service.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired
    private AuthEntryPoint unauthorizedHandler;
    // Ova klasa predstavlja konfiguraciju Spring Security-ja za post-service

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("http://localhost:5173"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                config.setAllowCredentials(true);
                return config;
            }))
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                // Javni — feed-service i sam profil prikazuju objave bez obzira na to da li je profil privatan ili ne, tako da su GET zahtevi ka /api/v1/post/** javno dostupni
                .requestMatchers(HttpMethod.GET, "/api/v1/post/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                // Ostalo zahteva JWT
                .anyRequest().authenticated()
            );

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

// Ova klasa predstavlja konfiguraciju Spring Security-ja za post-service.
// U ovoj konfiguraciji se definiše da su zahtevi ka /actuator/health
// i GET zahtevi ka /api/v1/post/** i /uploads/** javno dostupni, dok svi ostali zahtevi zahtevaju autentifikaciju putem JWT tokena.
// Takođe, konfiguracija onemogućava CSRF zaštitu (jer se koristi JWT) i omogućava CORS za frontend aplikaciju koja se nalazi na http://localhost:5173.
// U slučaju neautorizovanog pristupa, koristi se AuthEntryPoint koji vraća 401 Unauthorized odgovor.
// Na kraju, dodaje se AuthTokenFilter koji će se izvršavati pre UsernamePasswordAuthenticationFilter-a i proveravati JWT token u svakom zahtevu.