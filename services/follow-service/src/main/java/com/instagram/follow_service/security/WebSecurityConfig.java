package com.instagram.follow_service.security;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public InternalApiKeyFilter internalApiKeyFilter() {
        return new InternalApiKeyFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                                            .map(String::trim)
                                            .collect(Collectors.toList());
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(allowedOrigins);
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Internal-Api-Key"));
                config.setAllowCredentials(true);
                return config;
            }))
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()

                // Javni GET endpointi (brojevi) — dostupni svima
                .requestMatchers(HttpMethod.GET, "/api/v1/follow/*/count").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/follow/*/followers").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/follow/*/following").permitAll()

                // Interni endpointi — zasticeni InternalApiKeyFilter-om (ne JWT-om)
                .requestMatchers(HttpMethod.POST, "/api/v1/follow/requests/accept-all/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/follow/notifications/internal").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/follow/check-internal/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/follow/internal/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/follow/internal/**").permitAll()

                // Sve ostalo zahteva JWT
                .anyRequest().authenticated()
            );

        http.addFilterBefore(internalApiKeyFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}