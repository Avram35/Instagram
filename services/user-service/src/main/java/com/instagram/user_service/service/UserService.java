package com.instagram.user_service.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.instagram.user_service.dto.UserDto;
import com.instagram.user_service.entity.User;
import com.instagram.user_service.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public UserService(UserRepository userRepository, RestTemplate restTemplate) 
    {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }
    
    public UserDto createUser(UserDto input)
    {
        final User createdUser = User.builder()
                .id(input.getId())
                .username(input.getUsername() != null ? input.getUsername().trim().toLowerCase() : null)
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
        User user = userRepository.findByUsername(username.trim().toLowerCase())
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
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Корисник није пронађен."));

        if (user.getProfilePictureUrl() != null) 
        {
            deleteOldPicture(user.getProfilePictureUrl());
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
        boolean usernameChanged = false;
        String newUsername = null;

        if (userDto.getUsername() != null)
        {
            newUsername = userDto.getUsername().trim().toLowerCase();

            if(newUsername.length() < 1) 
            {
                throw new IllegalArgumentException("Корисничко име мора имати најмање 1 карактер.");
            }

            if(newUsername.length() > 30) 
            {
                throw new IllegalArgumentException("Корисничко име не може имати више од 30 карактера.");
            }

            if(!newUsername.matches("^(?=.*[a-zA-Z])[\\w.]+$"))
            {
                throw new IllegalArgumentException("Корисничко име може садржати само слова, бројеве, тачке и доње црте.");
            }

            if (!newUsername.equals(oldUsername) && userRepository.findByUsername(newUsername).isPresent()) {
                throw new IllegalArgumentException("Корисничко име је већ заузето.");
            }

            if (!newUsername.equals(oldUsername)) {
                usernameChanged = true;
            }

            existing.setUsername(newUsername);
        }

        if (userDto.getFname() != null)
        {
            String newFname = userDto.getFname().trim();
            if(newFname.length() < 2) 
            {
                throw new IllegalArgumentException("Име мора имати најмање 2 слова.");
            }
            existing.setFname(newFname);
        } 

        if (userDto.getLname() != null)
        {
            String newLname = userDto.getLname().trim();
            if(newLname.length() < 2) 
            {
                throw new IllegalArgumentException("Презиме мора имати најмање 2 слова.");
            }
            existing.setLname(newLname);
        }

        if (userDto.getBio() != null)
        {
            String newBio = userDto.getBio().trim();
            if(newBio.length() > 150) 
            {
                throw new IllegalArgumentException("Ваша биографија не може имати више од 150 карактера.");
            }
            existing.setBio(newBio);
        }
 
        if (userDto.getPrivateProfile() != null) 
        {
            existing.setPrivateProfile(userDto.getPrivateProfile());
        }

        if (usernameChanged) 
        {
            syncUsernameWithAuth(oldUsername, newUsername);
        }

        userRepository.save(existing);

        if (wasPrivate && nowPublic) 
        {
            acceptAllPendingRequests(id);
        }
    }

    private static final String UPLOAD_DIR = "/app/uploads/profiles/";
    private static final long MAX_PROFILE_PICTURE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png");

    public String uploadProfilePicture(String username, MultipartFile file) throws IOException 
    {
        if (file.isEmpty()) 
        {
            throw new IllegalArgumentException("Фајл је празан.");
        }

        if (file.getSize() > MAX_PROFILE_PICTURE_SIZE) 
        {
            throw new IllegalArgumentException("Слика не може бити већа од 5 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) 
        {
            throw new IllegalArgumentException("Дозвољени формати: JPEG, PNG.");
        }

        User user = userRepository.findByUsername(username.trim().toLowerCase()).orElseThrow(() -> new RuntimeException("Корисник није пронађен."));

        if (user.getProfilePictureUrl() != null) 
        {
            deleteOldPicture(user.getProfilePictureUrl());
        }

        String extension = getExtensionFromContentType(contentType);
        String filename = UUID.randomUUID() + extension;

        Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        Path filepath = uploadDir.resolve(filename).normalize();

        if (!filepath.startsWith(uploadDir)) 
        {
            throw new IOException("Неисправна путања фајла.");
        }

        Files.createDirectories(filepath.getParent());
        Files.write(filepath, file.getBytes());

        String url = "/uploads/profiles/" + filename;
        user.setProfilePictureUrl(url);
        userRepository.save(user);

        return url;
    }

    private void deleteOldPicture(String profilePictureUrl) 
    {
        try {
            String oldPic = profilePictureUrl.replace("/uploads/profiles/", "");
            Path oldFilePath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Path filePath = oldFilePath.resolve(oldPic).normalize();

            if(!filePath.startsWith(oldFilePath)) 
            {
                log.warn("Pokusaj brisanja fajla izvan upload direktorijuma: {}", filePath);
                return;
            }

            Files.deleteIfExists(filePath);

        } catch (IOException e) {
            log.warn("Neuspesno brisanje stare profilne slike: {}", e.getMessage());
        }
    }

    private String getExtensionFromContentType(String contentType) 
    {
        return switch (contentType) 
        {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            default           -> ".jpg";
        };
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
        } catch (Exception e) {
            log.error("Neuspesna sinhronizacija username-a sa auth-service: {}", e.getMessage());
            throw new RuntimeException("Промена корисничког имена тренутно није могућа. Покушајте поново.");
        }
    }

    private void acceptAllPendingRequests(Long userId) 
    {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                "http://follow-service:8083/api/v1/follow/requests/accept-all/" + userId,
                HttpMethod.POST,
                entity,
                Void.class
            );
        } catch (Exception e) {
            log.warn("Greska pri prihvatanju pending zahteva: {}", e.getMessage());
        }
    }

    public List<UserDto> searchUsers(String query, Long currentUserId) 
    {
        List<UserDto> results = userRepository
            .findByUsernameContainingIgnoreCaseOrFnameContainingIgnoreCaseOrLnameContainingIgnoreCase(query, query, query)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());

        if (currentUserId != null) 
        {
            results = results.stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .filter(user -> !isBlockedEitherWay(currentUserId, user.getId()))
                .collect(Collectors.toList());
        }

        return results;
    }
    
    private boolean isBlockedEitherWay(Long userId1, Long userId2) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "http://blok-service:8084/api/v1/block/internal/check-either/" + userId1 + "/" + userId2,
                HttpMethod.GET,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            return response.getBody() != null 
                && Boolean.TRUE.equals(response.getBody().get("blocked"));
        } catch (Exception e) {
            log.warn("Block servis nije dostupan: {}", e.getMessage());
            return false;
        }
    }
    /* 
    public List<UserDto> getAllUsers() 
    {
        return userRepository.findAll()
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }*/

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