package com.instagram.blok_service.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import com.instagram.blok_service.dto.BlockDto;
import com.instagram.blok_service.service.BlockService;

@ExtendWith(MockitoExtension.class)
class BlockControllerTest {

    @Mock
    private BlockService blockService;

    @InjectMocks
    private BlockController blockController;

    private UserDetails mockUser(String username) {
        UserDetails user = mock(UserDetails.class);
        when(user.getUsername()).thenReturn(username);
        return user;
    }

    private BlockDto testDto() {
        return BlockDto.builder()
                .id(1L).blockerId(1L).blockedId(2L)
                .createdAt(LocalDateTime.now()).build();
    }

    // ---------------- block -----------------------

    @Nested
    @DisplayName("block() testovi")
    class BlockTests {

        @Test
        @DisplayName("Uspesno blokiranje vraca 200")
        void block_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(blockService.getUserIdByUsername("ana")).thenReturn(1L);
            when(blockService.block(1L, 2L)).thenReturn(testDto());

            ResponseEntity<Map<String, String>> response = blockController.block(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody().get("message"));
        }

        @Test
        @DisplayName("Odblokiranje vraca 200")
        void unblock_shouldReturn200() {
            UserDetails currentUser = mockUser("ana");
            when(blockService.getUserIdByUsername("ana")).thenReturn(1L);
            doNothing().when(blockService).unblock(1L, 2L);

            ResponseEntity<Map<String, String>> response = blockController.unblock(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ----------------- check --------------------

    @Nested
    @DisplayName("check testovi")
    class CheckTests {

        @Test
        @DisplayName("isBlocked vraca true")
        void isBlocked_shouldReturnTrue() {
            UserDetails currentUser = mockUser("ana");
            when(blockService.getUserIdByUsername("ana")).thenReturn(1L);
            when(blockService.isBlocked(1L, 2L)).thenReturn(true);

            ResponseEntity<Map<String, Boolean>> response = blockController.isBlocked(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().get("blocked"));
        }

        @Test
        @DisplayName("isBlocked vraca false")
        void isBlocked_shouldReturnFalse() {
            UserDetails currentUser = mockUser("ana");
            when(blockService.getUserIdByUsername("ana")).thenReturn(1L);
            when(blockService.isBlocked(1L, 2L)).thenReturn(false);

            ResponseEntity<Map<String, Boolean>> response = blockController.isBlocked(2L, currentUser);

            assertFalse(response.getBody().get("blocked"));
        }

        @Test
        @DisplayName("isBlockedBy vraca true kada je currentUser blokiran od drugog")
        void isBlockedBy_shouldReturnTrue() {
            UserDetails currentUser = mockUser("ana");
            when(blockService.getUserIdByUsername("ana")).thenReturn(1L);
            when(blockService.isBlocked(2L, 1L)).thenReturn(true);

            ResponseEntity<Map<String, Boolean>> response = blockController.isBlockedBy(2L, currentUser);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().get("blocked"));
        }

        @Test
        @DisplayName("isBlockedEitherWay vraca true")
        void isBlockedEitherWay_shouldReturnTrue() {
            when(blockService.isBlockedEitherWay(1L, 2L)).thenReturn(true);

            ResponseEntity<Map<String, Boolean>> response = blockController.isBlockedEitherWay(1L, 2L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().get("blocked"));
        }

        @Test
        @DisplayName("isBlockedEitherWay vraca false")
        void isBlockedEitherWay_shouldReturnFalse() {
            when(blockService.isBlockedEitherWay(1L, 2L)).thenReturn(false);

            ResponseEntity<Map<String, Boolean>> response = blockController.isBlockedEitherWay(1L, 2L);

            assertFalse(response.getBody().get("blocked"));
        }
    }

    // ----------------- lista i brisanje --------------------

    @Test
    @DisplayName("getBlockedUsers vraca 200 sa listom")
    void getBlockedUsers_shouldReturn200() {
        UserDetails currentUser = mockUser("ana");
        when(blockService.getUserIdByUsername("ana")).thenReturn(1L);
        when(blockService.getBlockedUsers(1L)).thenReturn(List.of(testDto()));

        ResponseEntity<List<BlockDto>> response = blockController.getBlockedUsers(currentUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("deleteAllBlocksForUser vraca 204")
    void deleteAllBlocksForUser_shouldReturn204() {
        doNothing().when(blockService).deleteAllBlocksForUser(1L);

        ResponseEntity<Void> response = blockController.deleteAllBlocksForUser(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}