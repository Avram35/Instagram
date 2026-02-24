package com.instagram.user_service.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.instagram.user_service.dto.UserDto;
import com.instagram.user_service.entity.User;
import com.instagram.user_service.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) 
    {
        this.userRepository = userRepository;
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
         log.info("Updating user with id: {}", id);
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Корисник није пронађен."));

        if (userDto.getUsername() != null) existing.setUsername(userDto.getUsername());
        if (userDto.getFname() != null) existing.setFname(userDto.getFname());
        if (userDto.getLname() != null) existing.setLname(userDto.getLname());
        if (userDto.getBio() != null) existing.setBio(userDto.getBio());
        if (userDto.getProfilePictureUrl() != null) existing.setProfilePictureUrl(userDto.getProfilePictureUrl());
        if (userDto.getPrivateProfile() != null) existing.setPrivateProfile(userDto.getPrivateProfile());

        userRepository.save(existing);
    }

    public List<UserDto> searchUsers(String query) 
    {
        return userRepository
            .findByUsernameContainingIgnoreCaseOrFnameContainingIgnoreCaseOrLnameContainingIgnoreCase(query, query, query)
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
