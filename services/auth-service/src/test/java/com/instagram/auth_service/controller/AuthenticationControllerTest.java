package com.instagram.auth_service.controller;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.instagram.auth_service.dto.SignInRequest;
import com.instagram.auth_service.dto.SignUpRequest;
import com.instagram.auth_service.entity.User;
import com.instagram.auth_service.repository.UserRepository;
import com.instagram.auth_service.security.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AuthenticationController authController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "internalApiKey", "test-api-key");
    }

    // ==================== SIGN IN ====================

    @Nested
    @DisplayName("Sign In testovi")
    class SignInTests {

        @Test
        @DisplayName("Uspesna prijava vraca token")
        void signIn_WithValidCredentials_ShouldReturnToken() {
            SignInRequest request = new SignInRequest();
            request.setUsernameOrEmail("milan");
            request.setPassword("Sifra123!");

            Authentication authentication = mock(Authentication.class);
            UserDetails userDetails = mock(UserDetails.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetails.getUsername()).thenReturn("milan");
            when(jwtUtil.generateToken("milan")).thenReturn("jwt-token-123");

            ResponseEntity<?> response = authController.authenticateUser(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertEquals("jwt-token-123", body.get("token"));
        }

        @Test
        @DisplayName("Neispravni kredencijali vraca 401")
        void signIn_WithInvalidCredentials_ShouldReturn401() {
            SignInRequest request = new SignInRequest();
            request.setUsernameOrEmail("milan");
            request.setPassword("pogresna");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            ResponseEntity<?> response = authController.authenticateUser(request);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Prijava sa email-om trimuje i lowercase")
        void signIn_WithEmail_ShouldTrimAndLowercase() {
            SignInRequest request = new SignInRequest();
            request.setUsernameOrEmail("  milan@Test.COM  ");
            request.setPassword("Sifra123!");

            Authentication authentication = mock(Authentication.class);
            UserDetails userDetails = mock(UserDetails.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(userDetails.getUsername()).thenReturn("milan");
            when(jwtUtil.generateToken("milan")).thenReturn("token");

            ResponseEntity<?> response = authController.authenticateUser(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(authenticationManager).authenticate(
                    argThat(auth -> auth.getPrincipal().equals("milan@test.com"))
            );
        }
    }

    // ==================== SIGN UP ====================

    @Nested
    @DisplayName("Sign Up testovi")
    class SignUpTests {

        private SignUpRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new SignUpRequest();
            validRequest.setUsername("avram");
            validRequest.setEmail("avram@test.com");
            validRequest.setFname("milan");
            validRequest.setLname("avramovic");
            validRequest.setPassword("Sifra123!");
        }

        @Test
        @DisplayName("Uspesna registracija vraca 201")
        void signUp_WithValidData_ShouldReturn201() {
            when(userRepository.existsByUsername("avram")).thenReturn(false);
            when(userRepository.existsByEmail("avram@test.com")).thenReturn(false);
            when(encoder.encode("Sifra123!")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            ResponseEntity<?> response = authController.registerUser(validRequest);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Zauzeto korisnicko ime vraca 409")
        void signUp_WithExistingUsername_ShouldReturn409() {
            when(userRepository.existsByUsername("avram")).thenReturn(true);

            ResponseEntity<?> response = authController.registerUser(validRequest);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Zauzet email vraca 409")
        void signUp_WithExistingEmail_ShouldReturn409() {
            when(userRepository.existsByUsername("avram")).thenReturn(false);
            when(userRepository.existsByEmail("avram@test.com")).thenReturn(true);

            ResponseEntity<?> response = authController.registerUser(validRequest);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("User-service nedostupan rollback i 503")
        void signUp_WhenUserServiceDown_ShouldRollbackAndReturn503() {
            when(userRepository.existsByUsername("avram")).thenReturn(false);
            when(userRepository.existsByEmail("avram@test.com")).thenReturn(false);
            when(encoder.encode("Sifra123!")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            ResponseEntity<?> response = authController.registerUser(validRequest);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            verify(userRepository).delete(any(User.class));
        }

        @Test
        @DisplayName("Greska pri cuvanju u bazi vraca 500")
        void signUp_WhenDbSaveFails_ShouldReturn500() {
            when(userRepository.existsByUsername("avram")).thenReturn(false);
            when(userRepository.existsByEmail("avram@test.com")).thenReturn(false);
            when(encoder.encode("Sifra123!")).thenReturn("encoded");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new RuntimeException("DB error"));

            ResponseEntity<?> response = authController.registerUser(validRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        @DisplayName("Username se cuva kao lowercase")
        void signUp_ShouldSaveUsernameLowercase() {
            validRequest.setUsername("milan_123");

            when(userRepository.existsByUsername("milan_123")).thenReturn(false);
            when(userRepository.existsByEmail("avram@test.com")).thenReturn(false);
            when(encoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            authController.registerUser(validRequest);

            verify(userRepository).save(argThat(user -> user.getUsername().equals("milan_123")));
        }

        @Test
        @DisplayName("Ime i prezimee se salju ka user-service")
        void signUp_ShouldSendFnameAndLnameToUserService() {
            when(userRepository.existsByUsername("avram")).thenReturn(false);
            when(userRepository.existsByEmail("avram@test.com")).thenReturn(false);
            when(encoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
            when(restTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            authController.registerUser(validRequest);

            verify(restTemplate).postForEntity(
                    eq("http://user-service:8082/internal/api/v1/user"),
                    argThat(entity -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> body = ((org.springframework.http.HttpEntity<Map<String, Object>>) entity).getBody();
                        return "milan".equals(body.get("fname")) && "avramovic".equals(body.get("lname"));
                    }),
                    eq(Void.class)
            );
        }
    }

    // ==================== UPDATE USERNAME ====================

    @Nested
    @DisplayName("Update Username testovi")
    class UpdateUsernameTests {

        @Test
        @DisplayName("Uspesna promena username-a")
        void updateUsername_WithValidData_ShouldReturnOk() {
            Map<String, String> request = Map.of("oldUsername", "milan", "newUsername", "avram");

            User user = new User();
            user.setId(1L);
            user.setUsername("milan");

            when(userRepository.existsByUsername("avram")).thenReturn(false);
            when(userRepository.findByUsernameOrEmail("milan", "milan"))
                    .thenReturn(Optional.of(user));

            ResponseEntity<?> response = authController.updateUsername(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(userRepository).save(argThat(u -> u.getUsername().equals("avram")));
        }

        @Test
        @DisplayName("Novi username vec postoji vraca 409")
        void updateUsername_WithExistingUsername_ShouldReturn409() {
            Map<String, String> request = Map.of("oldUsername", "milan", "newUsername", "avram");

            when(userRepository.existsByUsername("avram")).thenReturn(true);

            ResponseEntity<?> response = authController.updateUsername(request);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("Prazan username vraca 400")
        void updateUsername_WithEmptyNewUsername_ShouldReturn400() {
            Map<String, String> request = Map.of("oldUsername", "milan", "newUsername", "  ");

            ResponseEntity<?> response = authController.updateUsername(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Null vrednosti vraca 400")
        void updateUsername_WithNullValues_ShouldReturn400() {
            java.util.HashMap<String, String> request = new java.util.HashMap<>();
            request.put("oldUsername", null);
            request.put("newUsername", null);

            ResponseEntity<?> response = authController.updateUsername(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ==================== DELETE ACCOUNT ====================

    @Nested
    @DisplayName("Delete Account testovi")
    class DeleteAccountTests {

        @Test
        @DisplayName("Uspesno brisanje naloga")
        void deleteAccount_ShouldDeleteFromAllServices() {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("milan");

            User user = new User();
            user.setId(1L);
            user.setUsername("milan");

            when(userRepository.findByUsernameOrEmail("milan", "milan"))
                    .thenReturn(Optional.of(user));

            ResponseEntity<?> response = authController.deleteAccount(currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(restTemplate, atLeast(5)).exchange(
                    anyString(), eq(org.springframework.http.HttpMethod.DELETE), any(), eq(Void.class)
            );
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("Korisnik ne postoji vraca 500")
        void deleteAccount_UserNotFound_ShouldReturn500() {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("nepostoji");

            when(userRepository.findByUsernameOrEmail("nepostoji", "nepostoji"))
                    .thenReturn(Optional.empty());

            ResponseEntity<?> response = authController.deleteAccount(currentUser);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }
}