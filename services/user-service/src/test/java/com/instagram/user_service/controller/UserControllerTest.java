package com.instagram.user_service.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import com.instagram.user_service.dto.UserDto;
import com.instagram.user_service.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        testUserDto = UserDto.builder()
                .id(1L)
                .username("avram")
                .fname("Milan")
                .lname("Avramovic")
                .bio("Bio")
                .privateProfile(false)
                .build();
    }

    // ==================== getUserById ====================

    @Nested
    @DisplayName("getUserById testovi")
    class GetUserByIdTests {

        @Test
        @DisplayName("Vraca 200 kada korisnik postoji")
        void getUserById_shouldReturn200_whenUserExists() {
            when(userService.getUserById(1L)).thenReturn(testUserDto);

            ResponseEntity<UserDto> response = userController.getUserById(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("avram", response.getBody().getUsername());
        }

        @Test
        @DisplayName("Vraca 404 kada korisnik ne postoji")
        void getUserById_shouldReturn404_whenUserNotFound() {
            when(userService.getUserById(99L)).thenThrow(new RuntimeException("Корисник није пронађен."));

            ResponseEntity<UserDto> response = userController.getUserById(99L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ==================== getUserByUsername ====================

    @Nested
    @DisplayName("getUserByUsername testovi")
    class GetUserByUsernameTests {

        @Test
        @DisplayName("Vraca 200 kada korisnik postoji")
        void getUserByUsername_shouldReturn200_whenUserExists() {
            when(userService.getUserByUsername("avram")).thenReturn(testUserDto);

            ResponseEntity<UserDto> response = userController.getUserByUsername("avram");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("avram", response.getBody().getUsername());
        }

        @Test
        @DisplayName("Vraca 404 kada korisnik ne postoji")
        void getUserByUsername_shouldReturn404_whenNotFound() {
            when(userService.getUserByUsername("ghost")).thenThrow(new RuntimeException("Корисник није пронађен."));

            ResponseEntity<UserDto> response = userController.getUserByUsername("ghost");

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ==================== searchUsers ====================

    @Nested
    @DisplayName("searchUsers testovi")
    class SearchUsersTests {

        @Test
        @DisplayName("Vraca 400 kada je query prazan")
        void searchUsers_shouldReturn400_whenQueryEmpty() {
            ResponseEntity<List<UserDto>> response = userController.searchUsers("", null);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verify(userService, never()).searchUsers(any(), any());
        }

        @Test
        @DisplayName("Vraca 400 kada je query samo razmaci")
        void searchUsers_shouldReturn400_whenQueryOnlySpaces() {
            ResponseEntity<List<UserDto>> response = userController.searchUsers("   ", null);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Vraca 200 kada je currentUser null")
        void searchUsers_shouldReturn200_whenCurrentUserNull() {
            when(userService.searchUsers(eq("milan"), isNull())).thenReturn(List.of(testUserDto));

            ResponseEntity<List<UserDto>> response = userController.searchUsers("milan", null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, response.getBody().size());
        }

        @Test
        @DisplayName("Vraca 200 sa rezultatima kada je query validan")
        void searchUsers_shouldReturn200_whenQueryValid() {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("avram");
            when(userService.getUserByUsername("avram")).thenReturn(testUserDto);
            when(userService.searchUsers(eq("milan"), eq(1L))).thenReturn(List.of(testUserDto));

            ResponseEntity<List<UserDto>> response = userController.searchUsers("milan", currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, response.getBody().size());
        }

        @Test
        @DisplayName("Vraca praznu listu kada nema rezultata")
        void searchUsers_shouldReturnEmptyList_whenNoResults() {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("milan");
            when(userService.getUserByUsername("milan")).thenReturn(testUserDto);
            when(userService.searchUsers(any(), any())).thenReturn(List.of());

            ResponseEntity<List<UserDto>> response = userController.searchUsers("xyz", currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().isEmpty());
        }
    }

    // ==================== updateUser ====================

    @Nested
    @DisplayName("updateUser testovi")
    class UpdateUserTests {

        @Test
        @DisplayName("Vraca 200 kada korisnik menja sopstveni profil")
        void updateUser_shouldReturn200_whenUpdatingOwnProfile() {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("avram");
            when(userService.getUserById(1L)).thenReturn(testUserDto);
            doNothing().when(userService).updateUser(eq(1L), any(UserDto.class));

            ResponseEntity<?> response = userController.updateUser(1L, testUserDto, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertNotNull(body.get("message"));
        }

        @Test
        @DisplayName("Vraca 403 kada korisnik menja tudji profil")
        void updateUser_shouldReturn403_whenUpdatingAnotherUsersProfile() {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("avram");
            UserDto otherUser = UserDto.builder().id(2L).username("otheruser").build();
            when(userService.getUserById(2L)).thenReturn(otherUser);

            ResponseEntity<?> response = userController.updateUser(2L, testUserDto, currentUser);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @DisplayName("Vraca 400 kada su podaci neispravni")
        void updateUser_shouldReturn400_whenInvalidData() {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("avram");
            when(userService.getUserById(1L)).thenReturn(testUserDto);
            doThrow(new IllegalArgumentException("Корисничко име је већ заузето."))
                    .when(userService).updateUser(eq(1L), any(UserDto.class));

            ResponseEntity<?> response = userController.updateUser(1L, testUserDto, currentUser);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Vraca 404 kada korisnik ne postoji")
        void updateUser_shouldReturn404_whenUserNotFound() {
            UserDetails currentUser = mock(UserDetails.class, withSettings().lenient());
            when(userService.getUserById(99L)).thenThrow(new RuntimeException("Корисник није пронађен."));

            ResponseEntity<?> response = userController.updateUser(99L, testUserDto, currentUser);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ==================== uploadProfilePicture ====================

    @Nested
    @DisplayName("uploadProfilePicture testovi")
    class UploadProfilePictureTests {

        @Test
        @DisplayName("Vraca 400 kada je fajl neispravan")
        void uploadProfilePicture_shouldReturn400_whenFileInvalid() throws Exception {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("avram");
            MultipartFile mockFile = mock(MultipartFile.class);
            when(userService.uploadProfilePicture(eq("avram"), any()))
                    .thenThrow(new IllegalArgumentException("Фајл је празан."));

            ResponseEntity<?> response = userController.uploadProfilePicture(mockFile, currentUser);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Vraca 200 i URL kada je upload uspesan")
        void uploadProfilePicture_shouldReturn200_whenUploadSuccessful() throws Exception {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("avram");
            MultipartFile mockFile = mock(MultipartFile.class);
            when(userService.uploadProfilePicture(eq("avram"), any()))
                    .thenReturn("/uploads/profiles/slika.jpg");

            ResponseEntity<?> response = userController.uploadProfilePicture(mockFile, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertEquals("/uploads/profiles/slika.jpg", body.get("url"));
        }

        @Test
        @DisplayName("Vraca 500 kada dodje do greske na serveru")
        void uploadProfilePicture_shouldReturn500_whenServerError() throws Exception {
            UserDetails currentUser = mock(UserDetails.class);
            when(currentUser.getUsername()).thenReturn("avram");
            MultipartFile mockFile = mock(MultipartFile.class);
            when(userService.uploadProfilePicture(eq("avram"), any()))
                    .thenThrow(new RuntimeException("greska"));

            ResponseEntity<?> response = userController.uploadProfilePicture(mockFile, currentUser);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }
}