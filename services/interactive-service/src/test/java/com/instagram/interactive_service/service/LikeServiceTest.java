package com.instagram.interactive_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.instagram.interactive_service.dto.LikeDto;
import com.instagram.interactive_service.dto.UserProfileDto;
import com.instagram.interactive_service.entity.Like;
import com.instagram.interactive_service.repository.LikeRepository;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock private LikeRepository likeRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private LikeService likeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(likeService, "internalApiKey", "test-key");
    }

    // ─── Helperi ────────────────────────────────────────────────────────────────

    private Like testLike(Long id, Long userId, Long postId) {
        return Like.builder()
                .id(id).userId(userId).postId(postId)
                .createdAt(LocalDateTime.now()).build();
    }

    private UserProfileDto userDto(Long id, boolean privateProfile) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(id);
        dto.setUsername("user" + id);
        dto.setPrivateProfile(privateProfile);
        return dto;
    }

    // ─── likePost ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("likePost() testovi")
    class LikePostTests {

        @Test
        @DisplayName("Vec lajkovana objava baca IllegalStateException")
        void likePost_alreadyLiked_shouldThrow() {
            when(likeRepository.existsByUserIdAndPostId(1L, 10L)).thenReturn(true);

            assertThrows(IllegalStateException.class, () -> likeService.likePost(1L, 10L));
            verify(likeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Vlasnik moze lajkovati svoju objavu — notifikacija se ne salje")
        void likePost_owner_shouldSucceed() {
            when(likeRepository.existsByUserIdAndPostId(1L, 10L)).thenReturn(false);
            // checkCanInteract + sendLikeNotification oba pozivaju isti URL — koristimo lenient
            lenient().when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("userId", 1L)));
            Like saved = testLike(1L, 1L, 10L);
            when(likeRepository.save(any())).thenReturn(saved);
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L, false));

            LikeDto result = likeService.likePost(1L, 10L);

            assertNotNull(result);
            verify(likeRepository).save(any());
        }

        @Test
        @DisplayName("Lajkovanje javne objave drugog korisnika uspeva")
        void likePost_publicProfile_shouldSucceed() {
            when(likeRepository.existsByUserIdAndPostId(1L, 10L)).thenReturn(false);
            // checkCanInteract + sendLikeNotification oba pozivaju isti URL
            lenient().when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("userId", 2L)));
            when(restTemplate.exchange(
                    contains("check-either/1/2"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(restTemplate.getForObject(contains("/user/id/2"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(2L, false));
            Like saved = testLike(1L, 1L, 10L);
            when(likeRepository.save(any())).thenReturn(saved);
            // toDto poziva /user/id/1
            when(restTemplate.getForObject(contains("/user/id/1"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L, false));

            LikeDto result = likeService.likePost(1L, 10L);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Blokiran korisnik baca IllegalArgumentException")
        void likePost_blocked_shouldThrow() {
            when(likeRepository.existsByUserIdAndPostId(1L, 10L)).thenReturn(false);
            when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("userId", 2L)));
            when(restTemplate.exchange(
                    contains("check-either"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", true)));

            assertThrows(IllegalArgumentException.class, () -> likeService.likePost(1L, 10L));
        }

        @Test
        @DisplayName("Privatni profil + ne prati baca IllegalArgumentException")
        void likePost_privateNotFollowing_shouldThrow() {
            when(likeRepository.existsByUserIdAndPostId(1L, 10L)).thenReturn(false);
            when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("userId", 2L)));
            when(restTemplate.exchange(
                    contains("check-either/1/2"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(restTemplate.getForObject(contains("/user/id/2"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(2L, true));
            when(restTemplate.exchange(
                    contains("check-internal"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("following", false)));

            assertThrows(IllegalArgumentException.class, () -> likeService.likePost(1L, 10L));
        }

        @Test
        @DisplayName("Privatni profil + prati — uspesno lajkovanje")
        void likePost_privateFollowing_shouldSucceed() {
            when(likeRepository.existsByUserIdAndPostId(1L, 10L)).thenReturn(false);
            lenient().when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("userId", 2L)));
            when(restTemplate.exchange(
                    contains("check-either/1/2"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(restTemplate.getForObject(contains("/user/id/2"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(2L, true));
            when(restTemplate.exchange(
                    contains("check-internal"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("following", true)));
            Like saved = testLike(1L, 1L, 10L);
            when(likeRepository.save(any())).thenReturn(saved);
            when(restTemplate.getForObject(contains("/user/id/1"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L, false));

            assertDoesNotThrow(() -> likeService.likePost(1L, 10L));
        }

        @Test
        @DisplayName("Post-service nedostupan — lajkovanje prolazi (greska ignorisana)")
        void likePost_postServiceDown_shouldProceed() {
            when(likeRepository.existsByUserIdAndPostId(1L, 10L)).thenReturn(false);
            when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new RuntimeException("Service down"));
            Like saved = testLike(1L, 1L, 10L);
            when(likeRepository.save(any())).thenReturn(saved);
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L, false));

            assertDoesNotThrow(() -> likeService.likePost(1L, 10L));
        }
    }

    // ─── unlikePost ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("unlikePost() testovi")
    class UnlikePostTests {

        @Test
        @DisplayName("Uspesno uklanjanje lajka")
        void unlikePost_shouldDelete() {
            Like like = testLike(1L, 1L, 10L);
            when(likeRepository.findByUserIdAndPostId(1L, 10L)).thenReturn(Optional.of(like));

            likeService.unlikePost(1L, 10L);

            verify(likeRepository).delete(like);
        }

        @Test
        @DisplayName("Korisnik nije lajkovao — baca RuntimeException")
        void unlikePost_notLiked_shouldThrow() {
            when(likeRepository.findByUserIdAndPostId(1L, 10L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> likeService.unlikePost(1L, 10L));
        }
    }

    // ─── hasLiked ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasLiked vraca true kada je lajkovano")
    void hasLiked_shouldReturnTrue() {
        when(likeRepository.existsByUserIdAndPostId(1L, 10L)).thenReturn(true);
        assertTrue(likeService.hasLiked(1L, 10L));
    }

    @Test
    @DisplayName("hasLiked vraca false kada nije lajkovano")
    void hasLiked_shouldReturnFalse() {
        when(likeRepository.existsByUserIdAndPostId(1L, 10L)).thenReturn(false);
        assertFalse(likeService.hasLiked(1L, 10L));
    }

    // ─── getLikesCount ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLikesCount() testovi")
    class LikesCountTests {

        @Test
        @DisplayName("requesterId null — vraca ukupan count iz repositorija")
        void getLikesCount_nullRequester_shouldReturnTotal() {
            when(likeRepository.countByPostId(10L)).thenReturn(5L);

            assertEquals(5L, likeService.getLikesCount(10L, null));
        }

        @Test
        @DisplayName("requesterId prisutan — filtrira blokirane")
        void getLikesCount_withRequester_shouldFilterBlocked() {
            Like l1 = testLike(1L, 2L, 10L);
            Like l2 = testLike(2L, 3L, 10L);
            when(likeRepository.findByPostId(10L)).thenReturn(List.of(l1, l2));
            when(restTemplate.exchange(
                    contains("check-either/1/2"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(restTemplate.exchange(
                    contains("check-either/1/3"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", true)));

            assertEquals(1L, likeService.getLikesCount(10L, 1L));
        }

        @Test
        @DisplayName("Blok-service nedostupan — racuna sve lajkove")
        void getLikesCount_blockServiceDown_shouldCountAll() {
            Like l1 = testLike(1L, 2L, 10L);
            when(likeRepository.findByPostId(10L)).thenReturn(List.of(l1));
            when(restTemplate.exchange(
                    contains("check-either"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new RuntimeException("down"));

            assertEquals(1L, likeService.getLikesCount(10L, 1L));
        }
    }

    // ─── getLikes ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getLikes vraca listu LikeDto")
    void getLikes_shouldReturnList() {
        Like like = testLike(1L, 1L, 10L);
        when(likeRepository.findByPostId(10L)).thenReturn(List.of(like));
        when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                .thenReturn(userDto(1L, false));

        List<LikeDto> result = likeService.getLikes(10L);

        assertEquals(1, result.size());
    }

    // ─── deleteAllByPostId / deleteAllByUserId ────────────────────────────────────

    @Test
    @DisplayName("deleteAllByPostId poziva repository")
    void deleteAllByPostId_shouldCallRepository() {
        likeService.deleteAllByPostId(10L);
        verify(likeRepository).deleteByPostId(10L);
    }

    @Test
    @DisplayName("deleteAllByUserId poziva repository")
    void deleteAllByUserId_shouldCallRepository() {
        likeService.deleteAllByUserId(1L);
        verify(likeRepository).deleteByUserId(1L);
    }

    // ─── getUserIdByUsername ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserIdByUsername vraca ID")
    void getUserIdByUsername_shouldReturnId() {
        when(restTemplate.getForObject(contains("ana"), eq(UserProfileDto.class)))
                .thenReturn(userDto(42L, false));

        assertEquals(42L, likeService.getUserIdByUsername("ana"));
    }

    @Test
    @DisplayName("getUserIdByUsername null odgovor baca RuntimeException")
    void getUserIdByUsername_null_shouldThrow() {
        when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(null);
        assertThrows(RuntimeException.class, () -> likeService.getUserIdByUsername("ana"));
    }

    @Test
    @DisplayName("getUserIdByUsername service down baca RuntimeException")
    void getUserIdByUsername_serviceDown_shouldThrow() {
        when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                .thenThrow(new RuntimeException("down"));
        assertThrows(RuntimeException.class, () -> likeService.getUserIdByUsername("ana"));
    }
}