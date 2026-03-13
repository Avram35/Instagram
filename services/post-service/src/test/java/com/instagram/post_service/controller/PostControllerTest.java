package com.instagram.post_service.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;

import com.instagram.post_service.dto.PostDto;
import com.instagram.post_service.dto.UpdatePostRequest;
import com.instagram.post_service.service.PostService;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private PostController postController;

    private UserDetails mockUser(String username) {
        UserDetails u = mock(UserDetails.class);
        when(u.getUsername()).thenReturn(username);
        return u;
    }

    private PostDto testDto(Long id) {
        return PostDto.builder()
                .id(id).userId(10L)
                .description("opis")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─── createPost ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createPost vraca 200 sa PostDto")
    void createPost_shouldReturn200() throws IOException {
        UserDetails user = mockUser("ana");
        MockMultipartFile file = new MockMultipartFile("files", "f.jpg", "image/jpeg", new byte[100]);
        when(postService.getUserIdByUsername("ana")).thenReturn(10L);
        when(postService.createPost(eq(10L), any(), anyList())).thenReturn(testDto(1L));

        ResponseEntity<?> response = postController.createPost(List.of(file), "opis", user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ─── updateDescription ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateDescription vraca 200 sa PostDto")
    void updateDescription_shouldReturn200() {
        UserDetails user = mockUser("ana");
        UpdatePostRequest req = new UpdatePostRequest();
        req.setDescription("novi opis");
        when(postService.getUserIdByUsername("ana")).thenReturn(10L);
        when(postService.updateDescription(1L, 10L, "novi opis")).thenReturn(testDto(1L));

        ResponseEntity<?> response = postController.updateDescription(1L, req, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ─── deletePost ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deletePost vraca 200 sa porukom")
    void deletePost_shouldReturn200() throws IOException {
        UserDetails user = mockUser("ana");
        when(postService.getUserIdByUsername("ana")).thenReturn(10L);
        doNothing().when(postService).deletePost(1L, 10L);

        ResponseEntity<?> response = postController.deletePost(1L, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("message"));
    }

    // ─── deleteMedia ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteMedia — poslednji medij vraca poruku o brisanju objave")
    void deleteMedia_lastMedia_shouldReturnMessage() throws IOException {
        UserDetails user = mockUser("ana");
        when(postService.getUserIdByUsername("ana")).thenReturn(10L);
        when(postService.deleteMedia(1L, 1L, 10L)).thenReturn(null);

        ResponseEntity<?> response = postController.deleteMedia(1L, 1L, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("message"));
    }

    @Test
    @DisplayName("deleteMedia — nije poslednji vraca PostDto")
    void deleteMedia_notLast_shouldReturnDto() throws IOException {
        UserDetails user = mockUser("ana");
        when(postService.getUserIdByUsername("ana")).thenReturn(10L);
        when(postService.deleteMedia(1L, 1L, 10L)).thenReturn(testDto(1L));

        ResponseEntity<?> response = postController.deleteMedia(1L, 1L, user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(PostDto.class, response.getBody());
    }

    // ─── getPost ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPost() testovi")
    class GetPostTests {

        @Test
        @DisplayName("Moze videti — vraca 200 sa PostDto")
        void getPost_canView_shouldReturn200() {
            UserDetails user = mockUser("ana");
            PostDto dto = testDto(1L);
            when(postService.getUserIdByUsername("ana")).thenReturn(10L);
            when(postService.getPostById(1L)).thenReturn(dto);
            when(postService.canViewPosts(10L, 10L)).thenReturn(true);

            ResponseEntity<?> response = postController.getPost(1L, user);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Ne moze videti — vraca 403")
        void getPost_cannotView_shouldReturn403() {
            UserDetails user = mockUser("ana");
            PostDto dto = testDto(1L);
            dto = PostDto.builder().id(1L).userId(99L).createdAt(LocalDateTime.now()).build();
            when(postService.getUserIdByUsername("ana")).thenReturn(10L);
            when(postService.getPostById(1L)).thenReturn(dto);
            when(postService.canViewPosts(10L, 99L)).thenReturn(false);

            ResponseEntity<?> response = postController.getPost(1L, user);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // ─── getPostsByUser ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPostsByUser() testovi")
    class GetPostsByUserTests {

        @Test
        @DisplayName("Moze videti — vraca 200 sa listom")
        void getPostsByUser_canView_shouldReturn200() {
            UserDetails user = mockUser("ana");
            when(postService.getUserIdByUsername("ana")).thenReturn(10L);
            when(postService.canViewPosts(10L, 20L)).thenReturn(true);
            when(postService.getPostsByUserId(20L)).thenReturn(List.of(testDto(1L)));

            ResponseEntity<?> response = postController.getPostsByUser(20L, user);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Ne moze videti — vraca 403")
        void getPostsByUser_cannotView_shouldReturn403() {
            UserDetails user = mockUser("ana");
            when(postService.getUserIdByUsername("ana")).thenReturn(10L);
            when(postService.canViewPosts(10L, 20L)).thenReturn(false);

            ResponseEntity<?> response = postController.getPostsByUser(20L, user);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // ─── getPostCount ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPostCount vraca 200 sa brojem objava")
    void getPostCount_shouldReturn200() {
        when(postService.getPostCount(10L)).thenReturn(7L);

        ResponseEntity<Map<String, Long>> response = postController.getPostCount(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(7L, response.getBody().get("count"));
    }

    // ─── interni endpointi ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getPostInternal — post postoji vraca 200")
    void getPostInternal_found_shouldReturn200() {
        when(postService.getPostById(1L)).thenReturn(testDto(1L));

        ResponseEntity<?> response = postController.getPostInternal(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("getPostInternal — post ne postoji vraca 404")
    void getPostInternal_notFound_shouldReturn404() {
        when(postService.getPostById(99L)).thenThrow(new RuntimeException("Не постоји"));

        ResponseEntity<?> response = postController.getPostInternal(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("getPostsByUserInternal vraca 200 sa listom")
    void getPostsByUserInternal_shouldReturn200() {
        when(postService.getPostsByUserId(10L)).thenReturn(List.of(testDto(1L)));

        ResponseEntity<List<PostDto>> response = postController.getPostsByUserInternal(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("deleteAllPostsByUser vraca 204")
    void deleteAllPostsByUser_shouldReturn204() throws IOException {
        doNothing().when(postService).deleteAllPostsByUser(10L);

        ResponseEntity<Void> response = postController.deleteAllPostsByUser(10L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @DisplayName("deleteAllPostsByUser — greska se tihо guta, vraca 204")
    void deleteAllPostsByUser_exception_shouldStillReturn204() throws IOException {
        doThrow(new IOException("disk error")).when(postService).deleteAllPostsByUser(10L);

        ResponseEntity<Void> response = postController.deleteAllPostsByUser(10L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}