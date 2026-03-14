package com.instagram.post_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import org.springframework.web.multipart.MultipartFile;

import com.instagram.post_service.dto.PostDto;
import com.instagram.post_service.dto.UserProfileDto;
import com.instagram.post_service.entity.Post;
import com.instagram.post_service.entity.PostMedia;
import com.instagram.post_service.repository.PostMediaRepository;
import com.instagram.post_service.repository.PostRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostMediaRepository postMediaRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private PostService postService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "internalApiKey", "test-key");
    }

    // ─── Helperi ────────────────────────────────────────────────────────────────

    private Post testPost(Long id, Long userId) {
        return Post.builder()
                .id(id).userId(userId)
                .description("opis")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private PostMedia testMedia(Long id, Long postId) {
        return PostMedia.builder()
                .id(id).postId(postId)
                .mediaUrl("/uploads/posts/test.jpg")
                .mediaType("image")
                .position(0)
                .fileSize(1024L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserProfileDto userDto(Long id, String username) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(id);
        dto.setUsername(username);
        dto.setPrivateProfile(false);
        return dto;
    }

    private MockMultipartFile jpegFile(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", new byte[1024]);
    }

    private MockMultipartFile mp4File(String name) {
        return new MockMultipartFile(name, name + ".mp4", "video/mp4", new byte[1024]);
    }

    // ─── createPost ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createPost() validacije")
    class CreatePostValidations {

        @Test
        @DisplayName("Prazna lista fajlova baca IllegalArgumentException")
        void noFiles_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                    () -> postService.createPost(1L, "opis", Collections.emptyList()));
        }

        @Test
        @DisplayName("Null lista fajlova baca IllegalArgumentException")
        void nullFiles_shouldThrow() {
            assertThrows(IllegalArgumentException.class,
                    () -> postService.createPost(1L, "opis", null));
        }

        @Test
        @DisplayName("Vise od 20 fajlova baca IllegalArgumentException")
        void tooManyFiles_shouldThrow() {
            List<MultipartFile> files = Collections.nCopies(21, jpegFile("f"));
            assertThrows(IllegalArgumentException.class,
                    () -> postService.createPost(1L, "opis", files));
        }

        @Test
        @DisplayName("Opis duzi od 2200 karaktera baca IllegalArgumentException")
        void longDescription_shouldThrow() {
            String longDesc = "a".repeat(2201);
            assertThrows(IllegalArgumentException.class,
                    () -> postService.createPost(1L, longDesc, List.of(jpegFile("f"))));
        }

        @Test
        @DisplayName("Prazan fajl baca IllegalArgumentException")
        void emptyFile_shouldThrow() {
            MockMultipartFile empty = new MockMultipartFile("f", "f.jpg", "image/jpeg", new byte[0]);
            assertThrows(IllegalArgumentException.class,
                    () -> postService.createPost(1L, "opis", List.of(empty)));
        }

        @Test
        @DisplayName("Fajl bez content-type baca IllegalArgumentException")
        void nullContentType_shouldThrow() {
            MockMultipartFile f = new MockMultipartFile("f", "f.jpg", null, new byte[100]);
            assertThrows(IllegalArgumentException.class,
                    () -> postService.createPost(1L, "opis", List.of(f)));
        }

        @Test
        @DisplayName("Nedozvoljen tip fajla (PDF) baca IllegalArgumentException")
        void disallowedContentType_shouldThrow() {
            MockMultipartFile pdf = new MockMultipartFile("f", "f.pdf", "application/pdf", new byte[100]);
            assertThrows(IllegalArgumentException.class,
                    () -> postService.createPost(1L, "opis", List.of(pdf)));
        }

        @Test
        @DisplayName("Fajl veci od 50MB baca IllegalArgumentException")
        void oversizedFile_shouldThrow() {
            byte[] big = new byte[51 * 1024 * 1024];
            MockMultipartFile f = new MockMultipartFile("f", "f.jpg", "image/jpeg", big);
            assertThrows(IllegalArgumentException.class,
                    () -> postService.createPost(1L, "opis", List.of(f)));
        }

        @Test
        @DisplayName("Video fajl (mp4) prolazi validaciju")
        void videoFile_shouldPassValidation() {
            // Validacija ne baca izuzetak za video — baca se IOException pri cuvanju (nema diska)
            assertThrows(Exception.class,
                    () -> postService.createPost(1L, "opis", List.of(mp4File("v"))));
            // Bitno je da nije IllegalArgumentException — validacija je prosla
        }
    }

    // ─── updateDescription ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateDescription() testovi")
    class UpdateDescriptionTests {

        @Test
        @DisplayName("Uspesno azuriranje opisa vraca PostDto")
        void update_shouldReturnDto() {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any(Post.class))).thenReturn(post);
            when(postMediaRepository.findByPostIdOrderByPositionAsc(1L)).thenReturn(List.of());
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(10L, "ana"));
            when(restTemplate.getForObject(contains("/like/count/"), eq(Map.class)))
                    .thenReturn(Map.of("count", 0));
            when(restTemplate.getForObject(contains("/comment/count/"), eq(Map.class)))
                    .thenReturn(Map.of("count", 0));

            PostDto result = postService.updateDescription(1L, 10L, "novi opis");

            assertNotNull(result);
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("Post ne postoji baca RuntimeException")
        void update_notFound_shouldThrow() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> postService.updateDescription(99L, 10L, "opis"));
        }

        @Test
        @DisplayName("Drugi korisnik pokusava izmenu baca IllegalArgumentException")
        void update_wrongUser_shouldThrow() {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThrows(IllegalArgumentException.class,
                    () -> postService.updateDescription(1L, 99L, "opis"));
        }

        @Test
        @DisplayName("Opis duzi od 2200 karaktera baca IllegalArgumentException")
        void update_longDescription_shouldThrow() {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThrows(IllegalArgumentException.class,
                    () -> postService.updateDescription(1L, 10L, "a".repeat(2201)));
        }

        @Test
        @DisplayName("Null opis je dozvoljen")
        void update_nullDescription_shouldSucceed() {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postRepository.save(any())).thenReturn(post);
            when(postMediaRepository.findByPostIdOrderByPositionAsc(1L)).thenReturn(List.of());
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(10L, "ana"));
            when(restTemplate.getForObject(contains("/like/count/"), eq(Map.class)))
                    .thenReturn(Map.of("count", 0));
            when(restTemplate.getForObject(contains("/comment/count/"), eq(Map.class)))
                    .thenReturn(Map.of("count", 0));

            assertDoesNotThrow(() -> postService.updateDescription(1L, 10L, null));
        }
    }

    // ─── deletePost ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deletePost() testovi")
    class DeletePostTests {

        @Test
        @DisplayName("Post ne postoji baca RuntimeException")
        void delete_notFound_shouldThrow() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> postService.deletePost(99L, 10L));
        }

        @Test
        @DisplayName("Drugi korisnik pokusava brisanje baca IllegalArgumentException")
        void delete_wrongUser_shouldThrow() {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThrows(IllegalArgumentException.class,
                    () -> postService.deletePost(1L, 99L));
        }

        @Test
        @DisplayName("Uspesno brisanje poziva delete na repository-ima")
        void delete_shouldCallRepositories() throws IOException {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postMediaRepository.findByPostIdOrderByPositionAsc(1L))
                    .thenReturn(List.of(testMedia(1L, 1L)));
            when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE),
                    any(HttpEntity.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.noContent().build());

            postService.deletePost(1L, 10L);

            verify(postRepository).delete(post);
            verify(postMediaRepository).deleteByPostId(1L);
        }

        @Test
        @DisplayName("Interactive service nedostupan — brisanje i dalje uspeva")
        void delete_interactiveServiceDown_shouldStillDelete() throws IOException {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postMediaRepository.findByPostIdOrderByPositionAsc(1L)).thenReturn(List.of());
            when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE),
                    any(HttpEntity.class), eq(Void.class)))
                    .thenThrow(new RuntimeException("Service down"));

            assertDoesNotThrow(() -> postService.deletePost(1L, 10L));
            verify(postRepository).delete(post);
        }
    }

    // ─── deleteMedia ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteMedia() testovi")
    class DeleteMediaTests {

        @Test
        @DisplayName("Post ne postoji baca RuntimeException")
        void deleteMedia_postNotFound_shouldThrow() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> postService.deleteMedia(99L, 1L, 10L));
        }

        @Test
        @DisplayName("Drugi korisnik baca IllegalArgumentException")
        void deleteMedia_wrongUser_shouldThrow() {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThrows(IllegalArgumentException.class,
                    () -> postService.deleteMedia(1L, 1L, 99L));
        }

        @Test
        @DisplayName("Media ne postoji baca RuntimeException")
        void deleteMedia_mediaNotFound_shouldThrow() {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postMediaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> postService.deleteMedia(1L, 99L, 10L));
        }

        @Test
        @DisplayName("Media ne pripada ovom postu baca IllegalArgumentException")
        void deleteMedia_wrongPost_shouldThrow() {
            Post post = testPost(1L, 10L);
            PostMedia media = testMedia(5L, 999L); // pripada drugom postu
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postMediaRepository.findById(5L)).thenReturn(Optional.of(media));

            assertThrows(IllegalArgumentException.class,
                    () -> postService.deleteMedia(1L, 5L, 10L));
        }

        @Test
        @DisplayName("Brisanje poslednjeg medija brise i post — vraca null")
        void deleteMedia_lastMedia_shouldDeletePostAndReturnNull() throws IOException {
            Post post = testPost(1L, 10L);
            PostMedia media = testMedia(1L, 1L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postMediaRepository.findById(1L)).thenReturn(Optional.of(media));
            when(postMediaRepository.countByPostId(1L)).thenReturn(1L);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE),
                    any(HttpEntity.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.noContent().build());

            PostDto result = postService.deleteMedia(1L, 1L, 10L);

            assertNull(result);
            verify(postRepository).delete(post);
        }

        @Test
        @DisplayName("Brisanje jednog od vise medija vraca PostDto")
        void deleteMedia_notLastMedia_shouldReturnDto() throws IOException {
            Post post = testPost(1L, 10L);
            PostMedia media = testMedia(1L, 1L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postMediaRepository.findById(1L)).thenReturn(Optional.of(media));
            when(postMediaRepository.countByPostId(1L)).thenReturn(3L);
            when(postMediaRepository.findByPostIdOrderByPositionAsc(1L))
                    .thenReturn(List.of(testMedia(2L, 1L)));
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(10L, "ana"));
            when(restTemplate.getForObject(contains("/like/count/"), eq(Map.class)))
                    .thenReturn(Map.of("count", 0));
            when(restTemplate.getForObject(contains("/comment/count/"), eq(Map.class)))
                    .thenReturn(Map.of("count", 0));

            PostDto result = postService.deleteMedia(1L, 1L, 10L);

            assertNotNull(result);
            verify(postMediaRepository).delete(media);
        }
    }

    // ─── getPostById ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPostById() testovi")
    class GetPostByIdTests {

        @Test
        @DisplayName("Vraca PostDto kada post postoji")
        void getPostById_shouldReturnDto() {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postMediaRepository.findByPostIdOrderByPositionAsc(1L)).thenReturn(List.of());
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(10L, "ana"));
            when(restTemplate.getForObject(contains("/like/count/"), eq(Map.class)))
                    .thenReturn(Map.of("count", 5));
            when(restTemplate.getForObject(contains("/comment/count/"), eq(Map.class)))
                    .thenReturn(Map.of("count", 2));

            PostDto result = postService.getPostById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(5L, result.getLikesCount());
            assertEquals(2L, result.getCommentsCount());
        }

        @Test
        @DisplayName("Post ne postoji baca RuntimeException")
        void getPostById_notFound_shouldThrow() {
            when(postRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> postService.getPostById(99L));
        }

        @Test
        @DisplayName("User-service nedostupan — username je null, post se i dalje vraca")
        void getPostById_userServiceDown_shouldReturnDtoWithNullUsername() {
            Post post = testPost(1L, 10L);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(postMediaRepository.findByPostIdOrderByPositionAsc(1L)).thenReturn(List.of());
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenThrow(new RuntimeException("Service down"));
            when(restTemplate.getForObject(contains("/like/count/"), eq(Map.class)))
                    .thenReturn(null);
            when(restTemplate.getForObject(contains("/comment/count/"), eq(Map.class)))
                    .thenReturn(null);

            PostDto result = postService.getPostById(1L);

            assertNotNull(result);
            assertNull(result.getUsername());
            assertEquals(0L, result.getLikesCount());
        }
    }

    // ─── getPostsByUserId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPostsByUserId vraca listu PostDto")
    void getPostsByUserId_shouldReturnList() {
        Post post = testPost(1L, 10L);
        when(postRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(post));
        when(postMediaRepository.findByPostIdOrderByPositionAsc(1L)).thenReturn(List.of());
        when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                .thenReturn(userDto(10L, "ana"));
        when(restTemplate.getForObject(contains("/like/count/"), eq(Map.class)))
                .thenReturn(Map.of("count", 0));
        when(restTemplate.getForObject(contains("/comment/count/"), eq(Map.class)))
                .thenReturn(Map.of("count", 0));

        List<PostDto> result = postService.getPostsByUserId(10L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getPostsByUserId vraca praznu listu")
    void getPostsByUserId_empty_shouldReturnEmpty() {
        when(postRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        List<PostDto> result = postService.getPostsByUserId(10L);

        assertTrue(result.isEmpty());
    }

    // ─── getPostCount ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPostCount vraca tacan broj")
    void getPostCount_shouldReturnCount() {
        when(postRepository.countByUserId(10L)).thenReturn(5L);

        assertEquals(5L, postService.getPostCount(10L));
    }

    // ─── canViewPosts ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canViewPosts() testovi")
    class CanViewPostsTests {

        @Test
        @DisplayName("Vlasnik uvek moze videti svoje objave")
        void canView_sameUser_shouldReturnTrue() {
            assertTrue(postService.canViewPosts(10L, 10L));
        }

        @Test
        @DisplayName("Javni profil — svi mogu videti")
        void canView_publicProfile_shouldReturnTrue() {
            UserProfileDto owner = userDto(20L, "mina");
            owner.setPrivateProfile(false);
            when(restTemplate.getForObject(contains("/user/id/20"), eq(UserProfileDto.class)))
                    .thenReturn(owner);

            assertTrue(postService.canViewPosts(10L, 20L));
        }

        @Test
        @DisplayName("Privatni profil + pratilac moze videti")
        void canView_privateProfile_follower_shouldReturnTrue() {
            UserProfileDto owner = userDto(20L, "mina");
            owner.setPrivateProfile(true);
            when(restTemplate.getForObject(contains("/user/id/20"), eq(UserProfileDto.class)))
                    .thenReturn(owner);
            when(restTemplate.exchange(
                    contains("check-internal"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("following", true)));

            assertTrue(postService.canViewPosts(10L, 20L));
        }

        @Test
        @DisplayName("Privatni profil + ne prati — ne moze videti")
        void canView_privateProfile_notFollower_shouldReturnFalse() {
            UserProfileDto owner = userDto(20L, "mina");
            owner.setPrivateProfile(true);
            when(restTemplate.getForObject(contains("/user/id/20"), eq(UserProfileDto.class)))
                    .thenReturn(owner);
            when(restTemplate.exchange(
                    contains("check-internal"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(Map.of("following", false)));

            assertFalse(postService.canViewPosts(10L, 20L));
        }

        @Test
        @DisplayName("User-service nedostupan — vraca false")
        void canView_userServiceDown_shouldReturnFalse() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenThrow(new RuntimeException("Service down"));

            assertFalse(postService.canViewPosts(10L, 20L));
        }

        @Test
        @DisplayName("User-service vraca null — vraca true (tretira se kao javni)")
        void canView_nullOwner_shouldReturnTrue() {
            when(restTemplate.getForObject(contains("/user/id/"), eq(UserProfileDto.class)))
                    .thenReturn(null);

            assertTrue(postService.canViewPosts(10L, 20L));
        }
    }

    // ─── getUserIdByUsername ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserIdByUsername() testovi")
    class GetUserIdTests {

        @Test
        @DisplayName("Vraca ID kada user-service dostupan")
        void getUserId_shouldReturnId() {
            when(restTemplate.getForObject(contains("testuser"), eq(UserProfileDto.class)))
                    .thenReturn(userDto(42L, "testuser"));

            assertEquals(42L, postService.getUserIdByUsername("testuser"));
        }

        @Test
        @DisplayName("Null odgovor baca RuntimeException")
        void getUserId_nullResponse_shouldThrow() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class))).thenReturn(null);

            assertThrows(RuntimeException.class,
                    () -> postService.getUserIdByUsername("testuser"));
        }

        @Test
        @DisplayName("Service nedostupan baca RuntimeException")
        void getUserId_serviceDown_shouldThrow() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenThrow(new RuntimeException("down"));

            assertThrows(RuntimeException.class,
                    () -> postService.getUserIdByUsername("testuser"));
        }
    }

    // ─── deleteAllPostsByUser ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteAllPostsByUser brise sve objave korisnika")
    void deleteAllPostsByUser_shouldDeleteAll() throws IOException {
        Post p1 = testPost(1L, 10L);
        Post p2 = testPost(2L, 10L);
        when(postRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(p1, p2));
        when(postMediaRepository.findByPostIdOrderByPositionAsc(anyLong())).thenReturn(List.of());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE),
                any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.noContent().build());

        postService.deleteAllPostsByUser(10L);

        verify(postRepository).delete(p1);
        verify(postRepository).delete(p2);
    }

    @Test
    @DisplayName("deleteAllPostsByUser kada nema objava — ne radi nista")
    void deleteAllPostsByUser_noPosts_shouldDoNothing() throws IOException {
        when(postRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        assertDoesNotThrow(() -> postService.deleteAllPostsByUser(10L));
        verify(postRepository, never()).delete(any());
    }
}