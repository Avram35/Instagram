package com.instagram.auth_service.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.instagram.auth_service.entity.User;
import com.instagram.auth_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Ucitavanje korisnika po username-u uspesno")
    void loadUserByUsername_ExistingUser_ShouldReturnUserDetails() {
        User user = new User();
        user.setId(1L);
        user.setUsername("milan");
        user.setEmail("milan@test.com");
        user.setPassword("encodedPassword123");

        when(userRepository.findByUsernameOrEmail("milan", "milan"))
                .thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("milan");

        assertNotNull(result);
        assertEquals("milan", result.getUsername());
        assertEquals("encodedPassword123", result.getPassword());
        assertTrue(result.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("Ucitavanje korisnika po email-u uspesno")
    void loadUserByUsername_ExistingEmail_ShouldReturnUserDetails() {
        User user = new User();
        user.setId(1L);
        user.setUsername("milan");
        user.setEmail("milan@test.com");
        user.setPassword("encodedPassword123");

        when(userRepository.findByUsernameOrEmail("milan@test.com", "milan@test.com"))
                .thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername("milan@test.com");

        assertNotNull(result);
        assertEquals("milan", result.getUsername());
    }

    @Test
    @DisplayName("Ucitavanje nepostojeceg korisnika baca UsernameNotFoundException")
    void loadUserByUsername_NonExistingUser_ShouldThrowException() {
        when(userRepository.findByUsernameOrEmail("nepostoji", "nepostoji"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nepostoji"));
    }

}