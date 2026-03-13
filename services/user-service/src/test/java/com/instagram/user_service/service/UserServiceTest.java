package com.instagram.user_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.instagram.user_service.dto.UserDto;
import com.instagram.user_service.entity.User;
import com.instagram.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "internalApiKey", "test-key");

        testUser = User.builder()
                .id(1L)
                .username("avram")
                .fname("Milan")
                .lname("Avramovic")
                .bio("Test bio")
                .profilePictureUrl(null)
                .privateProfile(false)
                .build();

        testUserDto = UserDto.builder()
                .id(1L)
                .username("avram")
                .fname("Milan")
                .lname("Avramovic")
                .bio("Test bio")
                .profilePictureUrl(null)
                .privateProfile(false)
                .build();
    }

    // ==================== createUser ====================

    @Test
    void createUser_shouldSaveAndReturnUser() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.createUser(testUserDto);

        assertNotNull(result);
        assertEquals("avram", result.getUsername());
        assertEquals("Milan", result.getFname());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_shouldSetPrivateProfileFalseWhenNull() {
        testUserDto.setPrivateProfile(null);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserDto result = userService.createUser(testUserDto);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    // ==================== getUserById ====================

    @Test
    void getUserById_shouldReturnUser_whenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("avram", result.getUsername());
    }

    @Test
    void getUserById_shouldThrowException_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getUserById(99L));

        assertTrue(ex.getMessage().contains("није пронађен"));
    }

    // ==================== getUserByUsername ====================

    @Test
    void getUserByUsername_shouldReturnUser_whenExists() {
        when(userRepository.findByUsername("avram")).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserByUsername("avram");

        assertNotNull(result);
        assertEquals("avram", result.getUsername());
    }

    @Test
    void getUserByUsername_shouldThrowException_whenNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getUserByUsername("nonexistent"));

        assertTrue(ex.getMessage().contains("није пронађен"));
    }

    @Test
    void getUserByUsername_shouldTrimAndLowercaseInput() {
        when(userRepository.findByUsername("avram")).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserByUsername("  Avram  ");

        assertNotNull(result);
        verify(userRepository).findByUsername("avram");
    }

    // ==================== deleteUser ====================

    @Test
    void deleteUser_shouldDeleteUser_whenExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_shouldThrowException_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).deleteById(any());
    }

    // ==================== updateUser ====================

    @Test
    void updateUser_shouldUpdateUsername_whenValid() {
        UserDto updateDto = UserDto.builder()
                .username("newusername")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("newusername")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        assertDoesNotThrow(() -> userService.updateUser(1L, updateDto));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_shouldThrowException_whenUsernameTooShort() {
        UserDto updateDto = UserDto.builder()
                .username("")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, updateDto));
    }

    @Test
    void updateUser_shouldThrowException_whenUsernameTooLong() {
        UserDto updateDto = UserDto.builder()
                .username("a".repeat(31))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, updateDto));
    }

    @Test
    void updateUser_shouldThrowException_whenUsernameInvalidChars() {
        UserDto updateDto = UserDto.builder()
                .username("invalid username!")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, updateDto));
    }

    @Test
    void updateUser_shouldThrowException_whenUsernameAlreadyTaken() {
        User anotherUser = User.builder().id(2L).username("takenname").build();
        UserDto updateDto = UserDto.builder().username("takenname").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("takenname")).thenReturn(Optional.of(anotherUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, updateDto));
    }

    @Test
    void updateUser_shouldThrowException_whenFnameTooShort() {
        UserDto updateDto = UserDto.builder().fname("A").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, updateDto));
    }

    @Test
    void updateUser_shouldThrowException_whenLnameTooShort() {
        UserDto updateDto = UserDto.builder().lname("B").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, updateDto));
    }

    @Test
    void updateUser_shouldThrowException_whenBioTooLong() {
        UserDto updateDto = UserDto.builder().bio("a".repeat(151)).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(1L, updateDto));
    }

    @Test
    void updateUser_shouldThrowException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateUser(99L, testUserDto));
    }

    @Test
    void updateUser_shouldUpdateBio_whenValid() {
        UserDto updateDto = UserDto.builder().bio("Nova biografija").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> userService.updateUser(1L, updateDto));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_shouldAcceptAllPendingRequests_whenProfileChangedToPublic() {
        testUser.setPrivateProfile(true);
        UserDto updateDto = UserDto.builder().privateProfile(false).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        assertDoesNotThrow(() -> userService.updateUser(1L, updateDto));
        verify(restTemplate).exchange(contains("accept-all"), eq(HttpMethod.POST), any(), eq(Void.class));
    }

    // ==================== searchUsers ====================

    @Test
    void searchUsers_shouldReturnResults_whenQueryMatches() {
        when(userRepository.findByUsernameContainingIgnoreCaseOrFnameContainingIgnoreCaseOrLnameContainingIgnoreCase(
                anyString(), anyString(), anyString()))
                .thenReturn(List.of(testUser));

        List<UserDto> results = userService.searchUsers("milan", null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("avram", results.get(0).getUsername());
    }

    @Test
    void searchUsers_shouldExcludeCurrentUser_whenLoggedIn() {
        when(userRepository.findByUsernameContainingIgnoreCaseOrFnameContainingIgnoreCaseOrLnameContainingIgnoreCase(
                anyString(), anyString(), anyString()))
                .thenReturn(List.of(testUser));

        List<UserDto> results = userService.searchUsers("test", 1L);

        assertTrue(results.isEmpty());
    }

    @Test
    void searchUsers_shouldReturnEmpty_whenNoMatch() {
        when(userRepository.findByUsernameContainingIgnoreCaseOrFnameContainingIgnoreCaseOrLnameContainingIgnoreCase(
                anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        List<UserDto> results = userService.searchUsers("zzzniko", null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ==================== uploadProfilePicture validacija ====================

    @Test
    void uploadProfilePicture_shouldThrow_whenFileEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> userService.uploadProfilePicture("avram", emptyFile));
    }

    @Test
    void uploadProfilePicture_shouldThrow_whenFileTooLarge() {
        byte[] largeContent = new byte[6 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", largeContent);

        assertThrows(IllegalArgumentException.class,
                () -> userService.uploadProfilePicture("avram", largeFile));
    }

    @Test
    void uploadProfilePicture_shouldThrow_whenInvalidContentType() {
        byte[] content = new byte[100];
        MockMultipartFile gifFile = new MockMultipartFile(
                "file", "test.gif", "image/gif", content);

        assertThrows(IllegalArgumentException.class,
                () -> userService.uploadProfilePicture("avram", gifFile));
    }

    @Test
    void uploadProfilePicture_shouldThrow_whenContentTypeNull() {
        byte[] content = new byte[100];
        MockMultipartFile fileNoType = new MockMultipartFile(
                "file", "test.jpg", null, content);

        assertThrows(IllegalArgumentException.class,
                () -> userService.uploadProfilePicture("avram", fileNoType));
    }

    // ==================== searchUsers - blok filtriranje ====================

    @Test
    void searchUsers_shouldFilterBlockedUser() {
        User blockedUser = User.builder()
                .id(2L).username("blocked").fname("Blocked").lname("User").build();

        when(userRepository
                .findByUsernameContainingIgnoreCaseOrFnameContainingIgnoreCaseOrLnameContainingIgnoreCase(
                        anyString(), anyString(), anyString()))
                .thenReturn(List.of(blockedUser));

        @SuppressWarnings("unchecked")
        ResponseEntity<java.util.Map<String, Object>> blockedResponse =
                (ResponseEntity<java.util.Map<String, Object>>) (ResponseEntity<?>) ResponseEntity.ok(
                        java.util.Map.of("blocked", true));

        when(restTemplate.exchange(
                contains("check-either"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(Class.class)))
                .thenReturn(blockedResponse);

        List<UserDto> results = userService.searchUsers("blocked", 1L);

        assertTrue(results.isEmpty());
    }

    @Test
    void searchUsers_shouldNotFilter_whenBlockServiceDown() {
        User user = User.builder()
                .id(2L).username("user2").fname("User").lname("Two").build();

        when(userRepository
                .findByUsernameContainingIgnoreCaseOrFnameContainingIgnoreCaseOrLnameContainingIgnoreCase(
                        anyString(), anyString(), anyString()))
                .thenReturn(List.of(user));

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), any(Class.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        List<UserDto> results = userService.searchUsers("user", 1L);

        assertEquals(1, results.size());
    }

    // ==================== getUserByFname / getUserByLname ====================

    @Test
    void getUserByFname_shouldReturnUser_whenExists() {
        when(userRepository.findByFname("Milan")).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserByFname("Milan");

        assertNotNull(result);
        assertEquals("Milan", result.getFname());
    }

    @Test
    void getUserByFname_shouldThrow_whenNotFound() {
        when(userRepository.findByFname("Nepostoji")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.getUserByFname("Nepostoji"));
    }

    @Test
    void getUserByLname_shouldReturnUser_whenExists() {
        when(userRepository.findByLname("Avramovic")).thenReturn(Optional.of(testUser));

        UserDto result = userService.getUserByLname("Avramovic");

        assertNotNull(result);
        assertEquals("Avramovic", result.getLname());
    }

    @Test
    void getUserByLname_shouldThrow_whenNotFound() {
        when(userRepository.findByLname("Nepostoji")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.getUserByLname("Nepostoji"));
    }

    // ==================== syncUsernameWithAuth ====================

    @Test
    void updateUser_shouldThrow_whenAuthServiceDown() {
        testUser.setUsername("oldname");
        UserDto updateDto = UserDto.builder().username("newname").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());
        when(restTemplate.exchange(
                contains("update-username"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThrows(RuntimeException.class,
                () -> userService.updateUser(1L, updateDto));
    }

    // ==================== deleteUser sa profilnom slikom ====================

    @Test
    void deleteUser_shouldDelete_whenNoPicture() {
        testUser.setProfilePictureUrl(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }
}