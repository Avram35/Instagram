package com.instagram.auth_service.service;

import java.util.Collections;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.instagram.auth_service.entity.User;
import com.instagram.auth_service.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) 
    {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException
    {
        String usernameOrEmailLower = usernameOrEmail.trim().toLowerCase();
        User user = userRepository.findByUsernameOrEmail(usernameOrEmailLower, usernameOrEmailLower).orElseThrow(() -> new UsernameNotFoundException("User not found with username or email: " + usernameOrEmailLower));

        return new org.springframework.security.core.userdetails.User
        (
            user.getUsername(),
            user.getPassword(),
            Collections.emptyList()
        );
    }
}
