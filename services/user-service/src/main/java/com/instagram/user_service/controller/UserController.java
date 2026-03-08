package com.instagram.user_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.instagram.user_service.dto.UserDto;
import com.instagram.user_service.service.UserService;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) 
    {
        this.userService = userService;
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) 
    {
        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username) 
    {
        try {
            return ResponseEntity.ok(userService.getUserByUsername(username));
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String query) 
    {
        if(query == null || query.trim().length() < 1) 
        {
            return ResponseEntity.badRequest().build();
        }
        List<UserDto> results = userService.searchUsers(query.trim());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserDto>> getAllUsers() 
    {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
        @PathVariable Long id, 
        @RequestBody UserDto userDto,
        @AuthenticationPrincipal UserDetails currentUser
    ) 
    {   
        try {
            UserDto profileOwner = userService.getUserById(id);
            if (!profileOwner.getUsername().equals(currentUser.getUsername())) 
            {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Немате дозволу за измену овог профила."));
            }
            userService.updateUser(id, userDto);
            return ResponseEntity.ok(Map.of("message", "Ажурирање корисника је успело."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Корисник није пронађен."));
        }
    }

    @PostMapping(value = "/profile-pic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePicture(
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        try {
            String url = userService.uploadProfilePicture(currentUser.getUsername(), file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Грешка при upload-u слике."));
        }
    }

}
