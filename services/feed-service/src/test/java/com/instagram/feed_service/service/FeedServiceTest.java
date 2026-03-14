package com.instagram.feed_service.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.instagram.feed_service.dto.FeedPostDto;
import com.instagram.feed_service.dto.FollowDto;
import com.instagram.feed_service.dto.UserProfileDto;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FeedService feedService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(feedService, "internalApiKey", "test-key");
    }

    // -------------------- Helperi -------------------

    private UserProfileDto userDto(Long id) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(id);
        return dto;
    }

    private FollowDto followDto(Long followingId) {
        FollowDto dto = new FollowDto();
        dto.setFollowingId(followingId);
        return dto;
    }

    private FeedPostDto postDto(Long postId, LocalDateTime createdAt) {
        FeedPostDto dto = new FeedPostDto();
        dto.setId(postId);
        dto.setCreatedAt(createdAt);
        return dto;
    }

    private void stubNotBlocked() {
        when(restTemplate.exchange(
                contains("check-either"), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("blocked", false)));
    }

    private void stubFollowing(List<FollowDto> list) {
        when(restTemplate.exchange(
                contains("/following"), eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(list));
    }

    private void stubPosts(Long userId, List<FeedPostDto> posts) {
        when(restTemplate.exchange(
                contains("/post/internal/user/" + userId), eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(posts));
    }

    // -------------------- getUserIdByUsername -------------------

    @Nested
    @DisplayName("getUserIdByUsername - greške")
    class UserServiceTests {

        @Test
        @DisplayName("Baca RuntimeException kada user-service vraca null")
        void userServiceNull_shouldThrow() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(null);

            assertThrows(RuntimeException.class,
                    () -> feedService.getFeed("mina", 0, 20));
        }

        @Test
        @DisplayName("Baca RuntimeException kada user-service nije dostupan")
        void userServiceDown_shouldThrow() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            assertThrows(RuntimeException.class,
                    () -> feedService.getFeed("mina", 0, 20));
        }
    }

    // -------------------- getFollowingIds -------------------

    @Nested
    @DisplayName("getFollowingIds - ivični slučajevi")
    class FollowingTests {

        @Test
        @DisplayName("Vraca praznu listu kada korisnik ne prati nikoga")
        void noFollowing_shouldReturnEmpty() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(Collections.emptyList());

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Vraca praznu listu kada follow-service vraca null body")
        void followingNull_shouldReturnEmpty() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            when(restTemplate.exchange(
                    contains("/following"), eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)))
                    .thenReturn(ResponseEntity.ok(null));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Vraca praznu listu kada follow-service nije dostupan")
        void followServiceDown_shouldReturnEmpty() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            when(restTemplate.exchange(
                    contains("/following"), eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertTrue(result.isEmpty());
        }
    }

    // -------------------- isBlockedEither -------------------

    @Nested
    @DisplayName("isBlockedEither - filtriranje blokiranih")
    class BlockFilterTests {

        @Test
        @DisplayName("Blokiran korisnik se ne pojavljuje u feedu")
        void blockedUser_shouldBeFiltered() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L)));
            when(restTemplate.exchange(
                    contains("check-either"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", true)));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Svi blokirani — vraca praznu listu")
        void allBlocked_shouldReturnEmpty() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L), followDto(3L)));
            when(restTemplate.exchange(
                    contains("check-either"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("blocked", true)));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Blok-service nedostupan — korisnik se prikazuje (false po defaultu)")
        void blockServiceDown_shouldIncludeUser() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L)));
            when(restTemplate.exchange(
                    contains("check-either"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenThrow(new RuntimeException("blok-service down"));
            stubPosts(2L, List.of(postDto(10L, LocalDateTime.now())));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Blok-service vraca null body — korisnik se prikazuje")
        void blockServiceNullBody_shouldIncludeUser() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L)));
            when(restTemplate.exchange(
                    contains("check-either"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(null));
            stubPosts(2L, List.of(postDto(10L, LocalDateTime.now())));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertEquals(1, result.size());
        }
    }

    // --------------- getPostsByUserId ------------

    @Nested
    @DisplayName("getPostsByUserId - greške")
    class PostServiceTests {

        @Test
        @DisplayName("Post-service nedostupan - preskace korisnika, nastavlja dalje")
        void postServiceDown_shouldSkipUser() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L), followDto(3L)));
            stubNotBlocked();
            when(restTemplate.exchange(
                    contains("/post/internal/user/2"), eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)))
                    .thenThrow(new RuntimeException("post-service down"));
            stubPosts(3L, List.of(postDto(30L, LocalDateTime.now())));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertEquals(1, result.size());
            assertEquals(30L, result.get(0).getId());
        }

        @Test
        @DisplayName("Post-service vraca null body - tretira se kao prazna lista")
        void postServiceNullBody_shouldTreatAsEmpty() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L)));
            stubNotBlocked();
            when(restTemplate.exchange(
                    contains("/post/internal/user/2"), eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    any(ParameterizedTypeReference.class)))
                    .thenReturn(ResponseEntity.ok(null));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertTrue(result.isEmpty());
        }
    }

    // ─── Sortiranje i paginacija ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Sortiranje i paginacija")
    class SortAndPaginationTests {

        @Test
        @DisplayName("Objave su sortirane hronoloski opadajuce (novije prve)")
        void posts_shouldBeSortedByDateDesc() {
            LocalDateTime older = LocalDateTime.now().minusDays(3);
            LocalDateTime newer = LocalDateTime.now().minusDays(1);

            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L)));
            stubNotBlocked();
            stubPosts(2L, List.of(postDto(1L, older), postDto(2L, newer)));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertEquals(2, result.size());
            assertEquals(2L, result.get(0).getId()); // noviji prvi
            assertEquals(1L, result.get(1).getId()); // stariji drugi
        }

        @Test
        @DisplayName("Objave od vise korisnika su spojene i sortirane")
        void multipleFollowing_shouldMergeAndSort() {
            LocalDateTime t1 = LocalDateTime.now().minusDays(3);
            LocalDateTime t2 = LocalDateTime.now().minusDays(1);
            LocalDateTime t3 = LocalDateTime.now().minusDays(2);

            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L), followDto(3L)));
            stubNotBlocked();
            stubPosts(2L, List.of(postDto(10L, t1)));
            stubPosts(3L, List.of(postDto(20L, t2), postDto(30L, t3)));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 20);

            assertEquals(3, result.size());
            assertEquals(20L, result.get(0).getId()); // t2 je najnoviji
            assertEquals(30L, result.get(1).getId()); // t3
            assertEquals(10L, result.get(2).getId()); // t1 je najstariji
        }

        @Test
        @DisplayName("Paginacija - prva strana (page=0, size=2) vraca 2 objave")
        void pagination_firstPage_shouldReturn2() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L)));
            stubNotBlocked();
            stubPosts(2L, List.of(
                    postDto(1L, LocalDateTime.now().minusDays(5)),
                    postDto(2L, LocalDateTime.now().minusDays(4)),
                    postDto(3L, LocalDateTime.now().minusDays(3)),
                    postDto(4L, LocalDateTime.now().minusDays(2)),
                    postDto(5L, LocalDateTime.now().minusDays(1))
            ));

            List<FeedPostDto> result = feedService.getFeed("mina", 0, 2);

            assertEquals(2, result.size());
            assertEquals(5L, result.get(0).getId()); // najnovije
        }

        @Test
        @DisplayName("Paginacija - druga strana (page=1, size=2)")
        void pagination_secondPage_shouldReturn2() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L)));
            stubNotBlocked();
            stubPosts(2L, List.of(
                    postDto(1L, LocalDateTime.now().minusDays(5)),
                    postDto(2L, LocalDateTime.now().minusDays(4)),
                    postDto(3L, LocalDateTime.now().minusDays(3)),
                    postDto(4L, LocalDateTime.now().minusDays(2)),
                    postDto(5L, LocalDateTime.now().minusDays(1))
            ));

            List<FeedPostDto> result = feedService.getFeed("mina", 1, 2);

            assertEquals(2, result.size());
            assertEquals(3L, result.get(0).getId());
        }

        @Test
        @DisplayName("Paginacija - page veci od broja objava vraca praznu listu")
        void pagination_pageOutOfBounds_shouldReturnEmpty() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L)));
            stubNotBlocked();
            stubPosts(2L, List.of(postDto(1L, LocalDateTime.now())));

            List<FeedPostDto> result = feedService.getFeed("mina", 999, 20);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Poslednja nepotpuna strana vraca preostale objave")
        void pagination_lastPartialPage_shouldReturnRemainder() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(userDto(1L));
            stubFollowing(List.of(followDto(2L)));
            stubNotBlocked();
            stubPosts(2L, List.of(
                    postDto(1L, LocalDateTime.now().minusDays(3)),
                    postDto(2L, LocalDateTime.now().minusDays(2)),
                    postDto(3L, LocalDateTime.now().minusDays(1))
            ));

            // page=1, size=2 → treba da vrati samo 1 preostalu
            List<FeedPostDto> result = feedService.getFeed("mina", 1, 2);

            assertEquals(1, result.size());
            assertEquals(1L, result.get(0).getId()); // najstariji
        }
    }
}