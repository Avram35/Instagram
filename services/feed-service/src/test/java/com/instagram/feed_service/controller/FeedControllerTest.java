package com.instagram.feed_service.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import com.instagram.feed_service.dto.FeedPostDto;
import com.instagram.feed_service.service.FeedService;

@ExtendWith(MockitoExtension.class)
class FeedControllerTest {

    @Mock
    private FeedService feedService;

    @InjectMocks
    private FeedController feedController;

    private UserDetails mockUser(String username) {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn(username);
        return user;
    }

    private FeedPostDto postDto(Long id) {
        FeedPostDto dto = new FeedPostDto();
        dto.setId(id);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    @Test
    @DisplayName("getFeed vraca 200 sa listom objava")
    void getFeed_shouldReturn200WithPosts() {
        UserDetails currentUser = mockUser("ana");
        when(feedService.getFeed("ana", 0, 20))
                .thenReturn(List.of(postDto(1L), postDto(2L)));

        ResponseEntity<List<FeedPostDto>> response =
                feedController.getFeed(currentUser, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    @DisplayName("getFeed vraca 200 sa praznom listom")
    void getFeed_noPost_shouldReturn200Empty() {
        UserDetails currentUser = mockUser("ana");
        when(feedService.getFeed("ana", 0, 20)).thenReturn(Collections.emptyList());

        ResponseEntity<List<FeedPostDto>> response =
                feedController.getFeed(currentUser, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("getFeed prosledjuje page i size servisu")
    void getFeed_shouldPassPageAndSize() {
        UserDetails currentUser = mockUser("ana");
        when(feedService.getFeed("ana", 2, 10)).thenReturn(List.of(postDto(5L)));

        ResponseEntity<List<FeedPostDto>> response =
                feedController.getFeed(currentUser, 2, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(feedService).getFeed("ana", 2, 10);
    }

    @Test
    @DisplayName("getFeed koristi username iz authentication principal-a")
    void getFeed_shouldUseUsernameFromPrincipal() {
        UserDetails currentUser = mockUser("mina");
        when(feedService.getFeed("mina", 0, 20)).thenReturn(Collections.emptyList());

        feedController.getFeed(currentUser, 0, 20);

        verify(feedService).getFeed("mina", 0, 20);
    }
}