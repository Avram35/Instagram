package com.instagram.interactive_service.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import com.instagram.interactive_service.dto.CommentDto;
import com.instagram.interactive_service.dto.CommentRequest;
import com.instagram.interactive_service.dto.LikeDto;
import com.instagram.interactive_service.service.CommentService;
import com.instagram.interactive_service.service.LikeService;

@ExtendWith(MockitoExtension.class)
class InteractiveControllerTest {

    // ===================================================================
    //  LikeController
    // ===================================================================

    @Nested
    @DisplayName("LikeController testovi")
    class LikeControllerTests {

        @Mock LikeService likeService;
        @InjectMocks LikeController likeController;

        private UserDetails mockUser(String username) {
            UserDetails u = mock(UserDetails.class);
            when(u.getUsername()).thenReturn(username);
            return u;
        }

        private LikeDto likeDto() {
            return LikeDto.builder().id(1L).userId(1L).postId(10L)
                    .createdAt(LocalDateTime.now()).build();
        }

        @Test
        @DisplayName("likePost vraca 200 sa LikeDto")
        void likePost_shouldReturn200() {
            UserDetails user = mockUser("ana");
            when(likeService.getUserIdByUsername("ana")).thenReturn(1L);
            when(likeService.likePost(1L, 10L)).thenReturn(likeDto());

            ResponseEntity<?> response = likeController.likePost(10L, user);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(LikeDto.class, response.getBody());
        }

        @Test
        @DisplayName("unlikePost vraca 200 sa porukom")
        void unlikePost_shouldReturn200() {
            UserDetails user = mockUser("ana");
            when(likeService.getUserIdByUsername("ana")).thenReturn(1L);
            doNothing().when(likeService).unlikePost(1L, 10L);

            ResponseEntity<?> response = likeController.unlikePost(10L, user);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((Map<?, ?>) response.getBody()).containsKey("message"));
        }

        @Test
        @DisplayName("hasLiked vraca true")
        void hasLiked_shouldReturnTrue() {
            UserDetails user = mockUser("ana");
            when(likeService.getUserIdByUsername("ana")).thenReturn(1L);
            when(likeService.hasLiked(1L, 10L)).thenReturn(true);

            ResponseEntity<Map<String, Boolean>> response = likeController.hasLiked(10L, user);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().get("liked"));
        }

        @Test
        @DisplayName("hasLiked vraca false")
        void hasLiked_shouldReturnFalse() {
            UserDetails user = mockUser("ana");
            when(likeService.getUserIdByUsername("ana")).thenReturn(1L);
            when(likeService.hasLiked(1L, 10L)).thenReturn(false);

            ResponseEntity<Map<String, Boolean>> response = likeController.hasLiked(10L, user);

            assertFalse(response.getBody().get("liked"));
        }

        @Test
        @DisplayName("getLikesCount sa null currentUser vraca count")
        void getLikesCount_nullUser_shouldReturnCount() {
            when(likeService.getLikesCount(10L, null)).thenReturn(5L);

            ResponseEntity<Map<String, Long>> response = likeController.getLikesCount(10L, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(5L, response.getBody().get("count"));
        }

        @Test
        @DisplayName("getLikesCount sa currentUser vraca filtrirani count")
        void getLikesCount_withUser_shouldReturnCount() {
            UserDetails user = mockUser("ana");
            when(likeService.getUserIdByUsername("ana")).thenReturn(1L);
            when(likeService.getLikesCount(10L, 1L)).thenReturn(3L);

            ResponseEntity<Map<String, Long>> response = likeController.getLikesCount(10L, user);

            assertEquals(3L, response.getBody().get("count"));
        }

        @Test
        @DisplayName("getLikes vraca listu")
        void getLikes_shouldReturnList() {
            when(likeService.getLikes(10L)).thenReturn(List.of(likeDto()));

            ResponseEntity<List<LikeDto>> response = likeController.getLikes(10L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, response.getBody().size());
        }

        @Test
        @DisplayName("deleteAllByPost (interni) vraca 204")
        void deleteAllByPost_shouldReturn204() {
            doNothing().when(likeService).deleteAllByPostId(10L);

            ResponseEntity<Void> response = likeController.deleteAllByPost(10L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        }

        @Test
        @DisplayName("deleteAllByUser (interni) vraca 204")
        void deleteAllByUser_shouldReturn204() {
            doNothing().when(likeService).deleteAllByUserId(1L);

            ResponseEntity<Void> response = likeController.deleteAllByUser(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        }
    }

    // ===================================================================
    //  CommentController
    // ===================================================================

    @Nested
    @DisplayName("CommentController testovi")
    class CommentControllerTests {

        @Mock CommentService commentService;
        @InjectMocks CommentController commentController;

        private UserDetails mockUser(String username) {
            UserDetails u = mock(UserDetails.class);
            when(u.getUsername()).thenReturn(username);
            return u;
        }

        private CommentDto commentDto() {
            return CommentDto.builder().id(1L).userId(1L).postId(10L)
                    .content("komentar").createdAt(LocalDateTime.now()).build();
        }

        private CommentRequest request(String content) {
            CommentRequest r = new CommentRequest();
            r.setContent(content);
            return r;
        }

        @Test
        @DisplayName("addComment vraca 200 sa CommentDto")
        void addComment_shouldReturn200() {
            UserDetails user = mockUser("ana");
            when(commentService.getUserIdByUsername("ana")).thenReturn(1L);
            when(commentService.addComment(1L, 10L, "komentar")).thenReturn(commentDto());

            ResponseEntity<?> response = commentController.addComment(10L, request("komentar"), user);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(CommentDto.class, response.getBody());
        }

        @Test
        @DisplayName("updateComment vraca 200 sa CommentDto")
        void updateComment_shouldReturn200() {
            UserDetails user = mockUser("ana");
            when(commentService.getUserIdByUsername("ana")).thenReturn(1L);
            when(commentService.updateComment(1L, 1L, "novi")).thenReturn(commentDto());

            ResponseEntity<?> response = commentController.updateComment(1L, request("novi"), user);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("deleteComment vraca 200 sa porukom")
        void deleteComment_shouldReturn200() {
            UserDetails user = mockUser("ana");
            when(commentService.getUserIdByUsername("ana")).thenReturn(1L);
            doNothing().when(commentService).deleteComment(1L, 1L);

            ResponseEntity<?> response = commentController.deleteComment(1L, user);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(((Map<?, ?>) response.getBody()).containsKey("message"));
        }

        @Test
        @DisplayName("getComments sa null currentUser vraca listu")
        void getComments_nullUser_shouldReturnList() {
            when(commentService.getComments(10L, null)).thenReturn(List.of(commentDto()));

            ResponseEntity<List<CommentDto>> response = commentController.getComments(10L, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(1, response.getBody().size());
        }

        @Test
        @DisplayName("getComments sa currentUser vraca listu")
        void getComments_withUser_shouldReturnList() {
            UserDetails user = mockUser("ana");
            when(commentService.getUserIdByUsername("ana")).thenReturn(1L);
            when(commentService.getComments(10L, 1L)).thenReturn(List.of(commentDto()));

            ResponseEntity<List<CommentDto>> response = commentController.getComments(10L, user);

            assertEquals(1, response.getBody().size());
        }

        @Test
        @DisplayName("getCommentsCount sa null user vraca count")
        void getCommentsCount_nullUser_shouldReturnCount() {
            when(commentService.getCommentsCount(10L, null)).thenReturn(4L);

            ResponseEntity<Map<String, Long>> response =
                    commentController.getCommentsCount(10L, null);

            assertEquals(4L, response.getBody().get("count"));
        }

        @Test
        @DisplayName("getCommentsCount sa currentUser vraca filtrirani count")
        void getCommentsCount_withUser_shouldReturnCount() {
            UserDetails user = mockUser("ana");
            when(commentService.getUserIdByUsername("ana")).thenReturn(1L);
            when(commentService.getCommentsCount(10L, 1L)).thenReturn(2L);

            ResponseEntity<Map<String, Long>> response =
                    commentController.getCommentsCount(10L, user);

            assertEquals(2L, response.getBody().get("count"));
        }

        @Test
        @DisplayName("deleteAllByPost (interni) vraca 204")
        void deleteAllByPost_shouldReturn204() {
            doNothing().when(commentService).deleteAllByPostId(10L);

            ResponseEntity<Void> response = commentController.deleteAllByPost(10L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        }

        @Test
        @DisplayName("deleteAllByUser (interni) vraca 204")
        void deleteAllByUser_shouldReturn204() {
            doNothing().when(commentService).deleteAllByUserId(1L);

            ResponseEntity<Void> response = commentController.deleteAllByUser(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        }
    }
}