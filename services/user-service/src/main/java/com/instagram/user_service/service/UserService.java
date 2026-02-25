package com.instagram.user_service.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.instagram.user_service.dto.UserDto;
import com.instagram.user_service.entity.User;
import com.instagram.user_service.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    @Value("${internal.api.key}")
    private String internalApiKey;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    public UserService(UserRepository userRepository, RestTemplate restTemplate) 
    {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }
    
    public UserDto createUser(UserDto input)
    {
        
        final User createdUser = User.builder()
                .id(input.getId())
                .username(input.getUsername())
                .fname(input.getFname())
                .lname(input.getLname())
                .bio(input.getBio())
                .profilePictureUrl(input.getProfilePictureUrl())
                .privateProfile(input.getPrivateProfile() != null ? input.getPrivateProfile() : false)
                .build();
            
        final User saved = userRepository.save(createdUser);
        return toDto(saved);

        
    } 

    public UserDto getUserById(Long id) 
    {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Корисник није пронађен."));
        return toDto(user);
    }

    public UserDto getUserByUsername(String username) 
    {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Корисник није пронађен."));
        return toDto(user);
    }

    public UserDto getUserByFname(String fname) 
    {
        User user = userRepository.findByFname(fname)
            .orElseThrow(() -> new RuntimeException("Корисник није пронађен."));
        return toDto(user);
    }

    public UserDto getUserByLname(String lname) 
    {
        User user = userRepository.findByLname(lname)
            .orElseThrow(() -> new RuntimeException("Корисник није пронађен."));
        return toDto(user);
    }

    public void deleteUser(Long id) 
    {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Корисник није пронађен.");
        }
        userRepository.deleteById(id);
    }

    public void updateUser(Long id, UserDto userDto) 
    {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Корисник није пронађен."));

        boolean wasPrivate = existing.getPrivateProfile() != null && existing.getPrivateProfile();
        boolean nowPublic = userDto.getPrivateProfile() != null && !userDto.getPrivateProfile();

        String oldUsername = existing.getUsername();

        if (userDto.getUsername() != null) existing.setUsername(userDto.getUsername());
        if (userDto.getFname() != null) existing.setFname(userDto.getFname());
        if (userDto.getLname() != null) existing.setLname(userDto.getLname());
        if (userDto.getBio() != null) existing.setBio(userDto.getBio());
        if (userDto.getProfilePictureUrl() != null) existing.setProfilePictureUrl(userDto.getProfilePictureUrl());
        if (userDto.getPrivateProfile() != null) existing.setPrivateProfile(userDto.getPrivateProfile());

        userRepository.save(existing);

        if (userDto.getUsername() != null && !userDto.getUsername().equals(oldUsername)) 
        {
        syncUsernameWithAuth(oldUsername, userDto.getUsername());
        }

        if (wasPrivate && nowPublic) 
        {
            acceptAllPendingRequests(id);
        }
    }

    private void syncUsernameWithAuth(String oldUsername, String newUsername) 
    {
        try {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.set("Content-Type", "application/json");

        Map<String, String> body = Map.of("oldUsername", oldUsername, "newUsername", newUsername);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(
                "http://auth-service:8081/api/v1/auth/internal/update-username",
                HttpMethod.PUT,
                entity,
                Void.class
            );
        } catch (Exception e) {}
    }

    private void acceptAllPendingRequests(Long userId) 
    {
        try {
            restTemplate.postForObject(
                "http://follow-service:8083/api/v1/follow/requests/accept-all/" + userId,
                null,
                Void.class
            );
        } catch (Exception e) {
        }
    }

    public List<UserDto> searchUsers(String query) 
    {
        return userRepository
            .findByUsernameContainingIgnoreCaseOrFnameContainingIgnoreCaseOrLnameContainingIgnoreCase(query, query, query)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public List<UserDto> getAllUsers() 
    {
        return userRepository.findAll()
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fname(user.getFname())
                .lname(user.getLname())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .privateProfile(user.getPrivateProfile())
                .build();
    }

    
}
