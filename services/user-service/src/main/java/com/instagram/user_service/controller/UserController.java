package com.instagram.user_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        List<UserDto> results = userService.searchUsers(query);
        return ResponseEntity.ok(results);
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) 
    {   
        try {
            userService.updateUser(id, userDto);
            return ResponseEntity.ok("Ажурирање корисника је успело.");
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Корисник није пронађен или ажурирање није успело", HttpStatus.NOT_FOUND);
        }
    }

}
