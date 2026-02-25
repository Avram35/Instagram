package com.instagram.auth_service.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.instagram.auth_service.dto.SignInRequest;
import com.instagram.auth_service.dto.SignUpRequest;
import com.instagram.auth_service.entity.User;
import com.instagram.auth_service.repository.UserRepository;
import com.instagram.auth_service.security.JwtUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public AuthenticationController(
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        PasswordEncoder encoder,
        JwtUtil jwtUtil,
        RestTemplate restTemplate
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody SignInRequest request) 
    {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsernameOrEmail(),
                    request.getPassword()
                )
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails.getUsername());

            return ResponseEntity.ok(Map.of("token", token));
        } catch (BadCredentialsException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Подаци које сте унели нису исправни."));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest user) 
    {
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Корисничко име је већ заузето!"));
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Email је већ заузет!"));
        }

        User newUser = new User();
        newUser.setUsername(user.getUsername());
        newUser.setEmail(user.getEmail());
        newUser.setFname(user.getFname());
        newUser.setLname(user.getLname());
        newUser.setPassword(encoder.encode(user.getPassword()));

        userRepository.save(newUser);

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("id", newUser.getId());
        userProfile.put("username", newUser.getUsername());
        userProfile.put("fname", newUser.getFname());
        userProfile.put("lname", newUser.getLname());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(userProfile, headers);

        restTemplate.postForEntity(
            "http://user-service:8082/internal/api/v1/user",
            entity,
            Void.class
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Map.of("message", "Успешно сте се регистровали!"));
    }

    @PutMapping("/internal/update-username")
    public ResponseEntity<?> updateUsername(@RequestBody Map<String, String> request, @RequestHeader("X-Internal-Api-Key") String apiKey) 
    {
        if (!internalApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Неважећи кључ."));
        }

        String oldUsername = request.get("oldUsername");
        String newUsername = request.get("newUsername");

        User user = userRepository.findByUsernameOrEmail(oldUsername, oldUsername)
            .orElseThrow(() -> new RuntimeException("Корисник није пронађен."));

        user.setUsername(newUsername);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Username ажуриран."));
    }
    
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);

            User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new RuntimeException("Корисник није пронађен."));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                "http://user-service:8082/internal/api/v1/user/" + user.getId(),
                HttpMethod.DELETE,
                entity,
                Void.class
            );

            userRepository.delete(user);

            return ResponseEntity.ok(Map.of("message", "Налог је успешно обрисан."));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Брисање налога није успело: " + e.getMessage()));
        }
    }
}