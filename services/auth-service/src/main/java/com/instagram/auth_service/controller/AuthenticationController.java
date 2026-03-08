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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.instagram.auth_service.dto.SignInRequest;
import com.instagram.auth_service.dto.SignUpRequest;
import com.instagram.auth_service.entity.User;
import com.instagram.auth_service.repository.UserRepository;
import com.instagram.auth_service.security.JwtUtil;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
                    request.getUsernameOrEmail().trim().toLowerCase(),
                    request.getPassword()
                )
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails.getUsername().trim().toLowerCase());

            return ResponseEntity.ok(Map.of("token", token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Подаци које сте унели нису исправни."));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest user) 
    {
        if (userRepository.existsByUsername(user.getUsername().trim().toLowerCase())) 
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Корисничко име је већ заузето!"));
        }

        if (userRepository.existsByEmail(user.getEmail().trim().toLowerCase())) 
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email је већ заузет!"));
        }

        try {
            User newUser = new User();
            newUser.setUsername(user.getUsername().trim().toLowerCase());
            newUser.setEmail(user.getEmail().trim().toLowerCase());
            newUser.setFname(user.getFname().trim());
            newUser.setLname(user.getLname().trim());
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Регистрација тренутно није могућа. Покушајте поново."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Успешно сте се регистровали!"));
    }
    @PutMapping("/internal/update-username")
    public ResponseEntity<?> updateUsername(@RequestBody Map<String, String> request) 
    {

        String oldUsername = request.get("oldUsername");
        String newUsername = request.get("newUsername");

        if (oldUsername == null || newUsername == null || newUsername.trim().isEmpty()) 
        {
            return ResponseEntity.badRequest().body(Map.of("error", "Унесите исправно корисничко име."));
        }

        if (userRepository.existsByUsername(newUsername.trim().toLowerCase())) 
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Корисничко име је већ заузето."));
        }


        User user = userRepository.findByUsernameOrEmail(oldUsername, oldUsername).orElseThrow(() -> new RuntimeException("Корисник није пронађен."));

        user.setUsername(newUsername.trim().toLowerCase());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Username ажуриран."));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal UserDetails currentUser) 
    {
        try {
            String username = currentUser.getUsername();

            User user = userRepository.findByUsernameOrEmail(username, username).orElseThrow(() -> new RuntimeException("Корисник није пронађен."));

            Long userId = user.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            DeleteUser("http://user-service:8082/internal/api/v1/user/" + userId, entity, "user-service");

            userRepository.delete(user);
            return ResponseEntity.ok(Map.of("message", "Налог је успешно обрисан."));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Брисање налога није успело. Покушајте поново."));
        }
    }

    private void DeleteUser(String url, HttpEntity<Void> entity, String serviceName) {
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
        } catch (Exception e) {
            log.warn("Неуспешно брисање из {}: {}", serviceName, e.getMessage());
        }
    }

}