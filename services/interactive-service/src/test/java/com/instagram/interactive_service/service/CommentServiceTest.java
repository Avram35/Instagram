package com.instagram.interactive_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.instagram.interactive_service.dto.CommentDto;
import com.instagram.interactive_service.dto.UserProfileDto;
import com.instagram.interactive_service.entity.Comment;
import com.instagram.interactive_service.repository.CommentRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(commentService, "internalApiKey", "test-key");
    }

    // ─── Helperi ────────────────────────────────────────────────────────────────

    private Comment testComment(Long id, Long userId, Long postId, String content) {
        return Comment.builder()
                .id(id).userId(userId).postId(postId).content(content)
                .createdAt(LocalDateTime.now()).build();
    }

    private UserProfileDto userDto(Long id, boolean privateProfile) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(id);
        dto.setUsername("user" + id);
        dto.setPrivateProfile(privateProfile);
        return dto;
    }

    // ─── addComment ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addComment() testovi")
    class AddCommentTests {

        @Test
        @DisplayName("Prazan komentar baca IllegalArgumentException")
        void addComment_emptyContent_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                    () -> commentService.addComment(1L, 10L, ""));
        }

        @Test
        @DisplayName("Null komentar baca IllegalArgumentException")
        void addComment_nullContent_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                    () -> commentService.addComment(1L, 10L, null));
        }

        @Test
        @DisplayName("Samo whitespace baca IllegalArgumentException")
        void addComment_whitespaceContent_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                    () -> commentService.addComment(1L, 10L, "   "));
        }

        @Test
        @DisplayName("Vlasnik moze komentarisati svoju objavu — notifikacija se ne salje")
        void addComment_owner_shouldSucceed() {
            
            // checkCanInteract i sendCommentNotification oba pozivaju /post/internal/10
            // koristimo lenient da izbegnemo UnnecessaryStubbing
            lenient().when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("userId", 1L)));
            Comment saved = testComment(1L, 1L, 10L, "super");
            when(commentRepository.save(any())).thenReturn(saved);
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L, false));

            CommentDto result = commentService.addComment(1L, 10L, "super");

            assertNotNull(result);
            verify(commentRepository).save(any());
        }

        @Test
        @DisplayName("Blokiran korisnik baca IllegalArgumentException")
        void addComment_blocked_shouldThrow() {
            when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("userId", 2L)));
            when(restTemplate.exchange(
                    contains("check-either"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", true)));

            assertThrows(IllegalArgumentException.class,
                    () -> commentService.addComment(1L, 10L, "komentar"));
        }

        @Test
        @DisplayName("Javni profil — komentarisanje uspeva")
        void addComment_publicProfile_shouldSucceed() {
            // lenient jer isti URL pozivaju i checkCanInteract i sendCommentNotification
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
            Comment saved = testComment(1L, 1L, 10L, "komentar");
            when(commentRepository.save(any())).thenReturn(saved);
            when(restTemplate.getForObject(contains("/user/id/1"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L, false));

            CommentDto result = commentService.addComment(1L, 10L, "komentar");
            assertNotNull(result);
        }

        @Test
        @DisplayName("Privatni profil + ne prati baca IllegalArgumentException")
        void addComment_privateNotFollowing_shouldThrow() {
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

            assertThrows(IllegalArgumentException.class,
                    () -> commentService.addComment(1L, 10L, "komentar"));
        }

        @Test
        @DisplayName("Privatni profil + prati — komentarisanje uspeva")
        void addComment_privateFollowing_shouldSucceed() {
            // lenient jer checkCanInteract i sendCommentNotification oba pozivaju isti URL
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
            Comment saved = testComment(1L, 1L, 10L, "komentar");
            when(commentRepository.save(any())).thenReturn(saved);
            when(restTemplate.getForObject(contains("/user/id/1"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L, false));

            assertDoesNotThrow(() -> commentService.addComment(1L, 10L, "komentar"));
        }

        @Test
        @DisplayName("Post-service nedostupan — komentar se i dalje dodaje")
        void addComment_postServiceDown_shouldProceed() {
            when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new RuntimeException("down"));
            Comment saved = testComment(1L, 1L, 10L, "komentar");
            when(commentRepository.save(any())).thenReturn(saved);
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L, false));

            assertDoesNotThrow(() -> commentService.addComment(1L, 10L, "komentar"));
        }
    }

    // ─── updateComment ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateComment() testovi")
    class UpdateCommentTests {

        @Test
        @DisplayName("Prazan sadrzaj baca IllegalArgumentException")
        void updateComment_empty_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                    () -> commentService.updateComment(1L, 1L, ""));
        }

        @Test
        @DisplayName("Komentar ne postoji baca RuntimeException")
        void updateComment_notFound_shouldThrow() {
            when(commentRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class,
                    () -> commentService.updateComment(99L, 1L, "novi"));
        }

        @Test
        @DisplayName("Drugi korisnik pokusava izmenu baca IllegalArgumentException")
        void updateComment_wrongUser_shouldThrow() {
            Comment comment = testComment(1L, 2L, 10L, "stari");
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            assertThrows(IllegalArgumentException.class,
                    () -> commentService.updateComment(1L, 1L, "novi"));
        }

        @Test
        @DisplayName("Uspesna izmena komentara vraca CommentDto")
        void updateComment_shouldReturnDto() {
            Comment comment = testComment(1L, 1L, 10L, "stari");
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
            when(commentRepository.save(any())).thenReturn(comment);
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L, false));

            CommentDto result = commentService.updateComment(1L, 1L, "novi komentar");

            assertNotNull(result);
            verify(commentRepository).save(any());
        }
    }

    // ─── deleteComment ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteComment() testovi")
    class DeleteCommentTests {

        @Test
        @DisplayName("Komentar ne postoji baca RuntimeException")
        void deleteComment_notFound_shouldThrow() {
            when(commentRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class,
                    () -> commentService.deleteComment(99L, 1L));
        }

        @Test
        @DisplayName("Vlasnik komentara moze obrisati")
        void deleteComment_commentOwner_shouldDelete() {
            Comment comment = testComment(1L, 1L, 10L, "komentar");
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            commentService.deleteComment(1L, 1L);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("Vlasnik objave moze obrisati tui komentar")
        void deleteComment_postOwner_shouldDelete() {
            Comment comment = testComment(1L, 2L, 10L, "komentar");
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
            when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("userId", 1L)));

            commentService.deleteComment(1L, 1L);

            verify(commentRepository).delete(comment);
        }

        @Test
        @DisplayName("Treci korisnik baca IllegalArgumentException")
        void deleteComment_unauthorized_shouldThrow() {
            Comment comment = testComment(1L, 2L, 10L, "komentar");
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
            when(restTemplate.exchange(
                    contains("/post/internal/10"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("userId", 3L)));

            assertThrows(IllegalArgumentException.class,
                    () -> commentService.deleteComment(1L, 1L));
        }

        @Test
        @DisplayName("Post-service nedostupan — treci korisnik ne moze brisati")
        void deleteComment_postServiceDown_shouldThrow() {
            Comment comment = testComment(1L, 2L, 10L, "komentar");
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
            when(restTemplate.exchange(
                    contains("/post/internal/"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new RuntimeException("down"));

            assertThrows(IllegalArgumentException.class,
                    () -> commentService.deleteComment(1L, 1L));
        }
    }

    // ─── getComments ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getComments() testovi")
    class GetCommentsTests {

        @Test
        @DisplayName("Vraca sve komentare kada requesterId null")
        void getComments_nullRequester_shouldReturnAll() {
            Comment c = testComment(1L, 2L, 10L, "komentar");
            when(commentRepository.findByPostIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(c));
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(2L, false));

            List<CommentDto> result = commentService.getComments(10L, null);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Filtrira komentare blokiranih korisnika")
        void getComments_filtersBlocked() {
            Comment c1 = testComment(1L, 2L, 10L, "od korisnika 2");
            Comment c2 = testComment(2L, 3L, 10L, "od korisnika 3");
            when(commentRepository.findByPostIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(c1, c2));
            when(restTemplate.exchange(
                    contains("check-either/1/2"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
            when(restTemplate.exchange(
                    contains("check-either/1/3"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", true)));
            when(restTemplate.getForObject(contains("/user/id/2"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(2L, false));

            List<CommentDto> result = commentService.getComments(10L, 1L);

            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getUserId());
        }

        @Test
        @DisplayName("Prazna lista komentara")
        void getComments_empty_shouldReturnEmpty() {
            when(commentRepository.findByPostIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

            List<CommentDto> result = commentService.getComments(10L, 1L);

            assertTrue(result.isEmpty());
        }
    }

    // ─── getCommentsCount ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCommentsCount null requesterId — vraca direktno iz repo")
    void getCommentsCount_nullRequester_shouldReturnRepoCount() {
        when(commentRepository.countByPostId(10L)).thenReturn(7L);

        assertEquals(7L, commentService.getCommentsCount(10L, null));
    }

    @Test
    @DisplayName("getCommentsCount filtrira blokirane")
    void getCommentsCount_filtersBlocked() {
        Comment c1 = testComment(1L, 2L, 10L, "a");
        Comment c2 = testComment(2L, 3L, 10L, "b");
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(c1, c2));
        when(restTemplate.exchange(
                contains("check-either/1/2"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
        when(restTemplate.exchange(
                contains("check-either/1/3"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("blocked", true)));

        assertEquals(1L, commentService.getCommentsCount(10L, 1L));
    }

    // ─── deleteAllByPostId / deleteAllByUserId ────────────────────────────────────

    @Test
    @DisplayName("deleteAllByPostId poziva repository")
    void deleteAllByPostId_shouldCallRepository() {
        commentService.deleteAllByPostId(10L);
        verify(commentRepository).deleteByPostId(10L);
    }

    @Test
    @DisplayName("deleteAllByUserId poziva repository")
    void deleteAllByUserId_shouldCallRepository() {
        commentService.deleteAllByUserId(1L);
        verify(commentRepository).deleteByUserId(1L);
    }

    // ─── getUserIdByUsername ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserIdByUsername vraca ID")
    void getUserIdByUsername_shouldReturnId() {
        when(restTemplate.getForObject(contains("ana"), eq(UserProfileDto.class)))
                .thenReturn(userDto(42L, false));
        assertEquals(42L, commentService.getUserIdByUsername("ana"));
    }

    @Test
    @DisplayName("getUserIdByUsername null odgovor baca RuntimeException")
    void getUserIdByUsername_null_shouldThrow() {
        when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(null);
        assertThrows(RuntimeException.class, () -> commentService.getUserIdByUsername("ana"));
    }

    @Test
    @DisplayName("getUserIdByUsername service down baca RuntimeException")
    void getUserIdByUsername_serviceDown_shouldThrow() {
        when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                .thenThrow(new RuntimeException("down"));
        assertThrows(RuntimeException.class, () -> commentService.getUserIdByUsername("ana"));
    }
}