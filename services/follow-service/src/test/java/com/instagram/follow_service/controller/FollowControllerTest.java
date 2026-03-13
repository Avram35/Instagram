package com.instagram.follow_service.controller;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import com.instagram.follow_service.dto.FollowCountDto;
import com.instagram.follow_service.dto.FollowRequestDto;
import com.instagram.follow_service.dto.NotificationDto;
import com.instagram.follow_service.service.FollowService;

@ExtendWith(MockitoExtension.class)
class FollowControllerTest {

    @Mock
    private FollowService followService;

    @InjectMocks
    private FollowController followController;

    private UserDetails mockUser(String username) {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn(username);
        return user;
    }

    // ----------------------- follow --------------------

    @Nested
    @DisplayName("follow() testovi")
    class FollowTests {

        @Test
        @DisplayName("Uspesno zapracivanje vraca 200")
        void follow_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            when(followService.follow(1L, 2L)).thenReturn(Map.of("message", "Успешно сте запратили корисника."));

            ResponseEntity<Map<String, String>> response = followController.follow(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody().get("message"));
        }

        @Test
        @DisplayName("Otpracivanje vraca 200")
        void unfollow_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            doNothing().when(followService).unfollow(1L, 2L);

            ResponseEntity<Map<String, String>> response = followController.unfollow(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Uklanjanje pratioce vraca 200")
        void removeFollower_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            doNothing().when(followService).removeFollower(1L, 2L);

            ResponseEntity<Map<String, String>> response = followController.removeFollower(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ------------------- zahtevi --------------------

    @Nested
    @DisplayName("Zahtevi za pracenje testovi")
    class RequestTests {

        @Test
        @DisplayName("Prihvatanje zahteva vraca 200")
        void acceptRequest_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            doNothing().when(followService).acceptRequest(1L, 1L);

            ResponseEntity<Map<String, String>> response = followController.acceptRequest(1L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Odbijanje zahteva vraca 200")
        void rejectRequest_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            doNothing().when(followService).rejectRequest(1L, 1L);

            ResponseEntity<Map<String, String>> response = followController.rejectRequest(1L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Lista pending zahteva vraca 200")
        void getPendingRequests_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            when(followService.getPendingRequests(1L)).thenReturn(List.of());

            ResponseEntity<List<FollowRequestDto>> response = followController.getPendingRequests(currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("hasPendingRequest vraca 200 sa boolean")
        void hasPendingRequest_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            when(followService.hasPendingRequest(1L, 2L)).thenReturn(true);

            ResponseEntity<Map<String, Boolean>> response = followController.hasPendingRequest(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().get("pending"));
        }
    }

    // ----------------- liste i brojevi ---------------------

    @Nested
    @DisplayName("Liste i brojevi testovi")
    class ListTests {

        @Test
        @DisplayName("getFollowers za javan profil vraca 200")
        void getFollowers_publicProfile_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            when(followService.canViewFollowList(2L, 1L)).thenReturn(true);
            when(followService.getFollowers(2L)).thenReturn(List.of());

            ResponseEntity<?> response = followController.getFollowers(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("getFollowers za privatan profil vraca 403")
        void getFollowers_privateProfile_shouldReturn403() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            when(followService.canViewFollowList(2L, 1L)).thenReturn(false);

            ResponseEntity<?> response = followController.getFollowers(2L, currentUser);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("getFollowing za javan profil vraca 200")
        void getFollowing_publicProfile_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            when(followService.canViewFollowList(2L, 1L)).thenReturn(true);
            when(followService.getFollowing(2L)).thenReturn(List.of());

            ResponseEntity<?> response = followController.getFollowing(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("getFollowCount vraca 200")
        void getFollowCount_shouldReturn200() {
            FollowCountDto dto = FollowCountDto.builder()
                    .userId(1L).followersCount(5L).followingCount(3L).build();
            when(followService.getFollowCount(1L)).thenReturn(dto);

            ResponseEntity<FollowCountDto> response = followController.getFollowCount(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(5L, response.getBody().getFollowersCount());
        }

        @Test
        @DisplayName("isFollowing vraca 200 sa boolean")
        void isFollowing_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(followService.getUserIdByUsername("ana")).thenReturn(1L);
            when(followService.isFollowing(1L, 2L)).thenReturn(true);

            ResponseEntity<Map<String, Boolean>> response = followController.isFollowing(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().get("following"));
        }
    }

    // --------------- interni endpointi -----------------

    @Nested
    @DisplayName("Interni endpointi testovi")
    class InternalTests {

        @Test
        @DisplayName("internalUnfollow vraca 204")
        void internalUnfollow_shouldReturn204() {
            doNothing().when(followService).unfollow(1L, 2L);

            ResponseEntity<Void> response = followController.internalUnfollow(1L, 2L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        }

        @Test
        @DisplayName("internalUnfollow ne baca exception kada ne prati")
        void internalUnfollow_notFollowing_shouldNotThrow() {
            doThrow(new RuntimeException("Ne prati")).when(followService).unfollow(1L, 2L);

            assertDoesNotThrow(() -> followController.internalUnfollow(1L, 2L));
        }

        @Test
        @DisplayName("createInternalNotification vraca 204")
        void createInternalNotification_shouldReturn204() {
            doNothing().when(followService).createInternalNotification(any());

            ResponseEntity<Void> response = followController.createInternalNotification(
                    Map.of("type", "LIKE", "recipientId", 1, "senderId", 2));

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        }

        @Test
        @DisplayName("acceptAllPending vraca 204")
        void acceptAllPending_shouldReturn204() {
            doNothing().when(followService).acceptAllPendingRequests(1L);

            ResponseEntity<Void> response = followController.acceptAllPending(1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        }

        @Test
        @DisplayName("checkInternal vraca 200 sa boolean")
        void checkInternal_shouldReturn200() {
            when(followService.checkInternalFollow(1L, 2L)).thenReturn(true);

            ResponseEntity<Map<String, Boolean>> response = followController.checkInternal(1L, 2L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().get("following"));
        }
    }

    // --------------- notifikacije -----------------

    @Test
    @DisplayName("getNotifications vraca 200")
    void getNotifications_shouldReturn200() {
        UserDetails currentUser = mockUser("ana");
        when(followService.getUserIdByUsername("ana")).thenReturn(1L);
        when(followService.getNotifications(1L)).thenReturn(List.of());

        ResponseEntity<List<NotificationDto>> response = followController.getNotifications(currentUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}