package com.instagram.blok_service.service;

import java.time.LocalDateTime;
import java.util.List;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.instagram.blok_service.dto.BlockDto;
import com.instagram.blok_service.dto.UserProfileDto;
import com.instagram.blok_service.entity.Block;
import com.instagram.blok_service.repository.BlockRepository;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock private BlockRepository blockRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private BlockService blockService;

    private Block testBlock;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(blockService, "internalApiKey", "test-key");

        testBlock = Block.builder()
                .id(1L)
                .blockerId(1L)
                .blockedId(2L)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==================== block() ====================

    @Nested
    @DisplayName("block() testovi")
    class BlockTests {

        @Test
        @DisplayName("Uspesno blokiranje vraca BlockDto")
        void block_shouldSaveAndReturnDto() {
            when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);
            when(blockRepository.save(any(Block.class))).thenReturn(testBlock);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.noContent().build());

            BlockDto result = blockService.block(1L, 2L);

            assertNotNull(result);
            assertEquals(1L, result.getBlockerId());
            assertEquals(2L, result.getBlockedId());
            verify(blockRepository).save(any(Block.class));
        }

        @Test
        @DisplayName("Blokiranje samog sebe baca IllegalArgumentException")
        void block_self_shouldThrowIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () -> blockService.block(1L, 1L));
            verify(blockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Vec blokiran korisnik baca IllegalStateException")
        void block_alreadyBlocked_shouldThrowIllegalState() {
            when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

            assertThrows(IllegalStateException.class, () -> blockService.block(1L, 2L));
            verify(blockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Follow servis nedostupan - blokiranje i dalje uspeva")
        void block_followServiceDown_shouldStillBlock() {
            when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);
            when(blockRepository.save(any(Block.class))).thenReturn(testBlock);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            assertDoesNotThrow(() -> blockService.block(1L, 2L));
            verify(blockRepository).save(any(Block.class));
        }

        @Test
        @DisplayName("Blokiranje poziva removeFollow u oba smera")
        void block_shouldCallRemoveFollowBothWays() {
            when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);
            when(blockRepository.save(any(Block.class))).thenReturn(testBlock);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.noContent().build());

            blockService.block(1L, 2L);

            verify(restTemplate, times(2)).exchange(
                    anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
        }
    }

    // ==================== unblock() ====================

    @Nested
    @DisplayName("unblock() testovi")
    class UnblockTests {

        @Test
        @DisplayName("Uspesno odblokiranje brise block")
        void unblock_shouldDelete() {
            when(blockRepository.findByBlockerIdAndBlockedId(1L, 2L)).thenReturn(Optional.of(testBlock));

            blockService.unblock(1L, 2L);

            verify(blockRepository).delete(testBlock);
        }

        @Test
        @DisplayName("Odblokiranje neblokiranog korisnika baca RuntimeException")
        void unblock_notBlocked_shouldThrow() {
            when(blockRepository.findByBlockerIdAndBlockedId(1L, 2L)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> blockService.unblock(1L, 2L));
            verify(blockRepository, never()).delete(any());
        }
    }

    // ==================== isBlocked() ====================

    @Nested
    @DisplayName("isBlocked() testovi")
    class IsBlockedTests {

        @Test
        @DisplayName("Vraca true kada je korisnik blokiran")
        void isBlocked_shouldReturnTrue() {
            when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

            assertTrue(blockService.isBlocked(1L, 2L));
        }

        @Test
        @DisplayName("Vraca false kada korisnik nije blokiran")
        void isBlocked_shouldReturnFalse() {
            when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);

            assertFalse(blockService.isBlocked(1L, 2L));
        }
    }

    // ==================== isBlockedEitherWay() ====================

    @Nested
    @DisplayName("isBlockedEitherWay() testovi")
    class IsBlockedEitherWayTests {

        @Test
        @DisplayName("Vraca true kada bloker blokira drugog")
        void isBlockedEitherWay_blockerBlocked_shouldReturnTrue() {
            when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

            assertTrue(blockService.isBlockedEitherWay(1L, 2L));
        }

        @Test
        @DisplayName("Vraca true kada je drugi blokiran od prvog")
        void isBlockedEitherWay_reverseBlocked_shouldReturnTrue() {
            when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);
            when(blockRepository.existsByBlockerIdAndBlockedId(2L, 1L)).thenReturn(true);

            assertTrue(blockService.isBlockedEitherWay(1L, 2L));
        }

        @Test
        @DisplayName("Vraca false kada nema bloka ni u jednom smeru")
        void isBlockedEitherWay_noBlock_shouldReturnFalse() {
            when(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);
            when(blockRepository.existsByBlockerIdAndBlockedId(2L, 1L)).thenReturn(false);

            assertFalse(blockService.isBlockedEitherWay(1L, 2L));
        }
    }

    // ==================== getBlockedUsers() ====================

    @Test
    @DisplayName("getBlockedUsers vraca listu blokiranih")
    void getBlockedUsers_shouldReturnList() {
        when(blockRepository.findByBlockerId(1L)).thenReturn(List.of(testBlock));

        List<BlockDto> result = blockService.getBlockedUsers(1L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getBlockedId());
    }

    @Test
    @DisplayName("getBlockedUsers vraca praznu listu kada nema blokiranih")
    void getBlockedUsers_empty_shouldReturnEmptyList() {
        when(blockRepository.findByBlockerId(1L)).thenReturn(List.of());

        List<BlockDto> result = blockService.getBlockedUsers(1L);

        assertTrue(result.isEmpty());
    }

    // ==================== deleteAllBlocksForUser() ====================

    @Test
    @DisplayName("deleteAllBlocksForUser poziva repository")
    void deleteAllBlocksForUser_shouldCallRepository() {
        when(blockRepository.deleteAllByUserId(1L)).thenReturn(3L);

        blockService.deleteAllBlocksForUser(1L);

        verify(blockRepository).deleteAllByUserId(1L);
    }

    @Test
    @DisplayName("deleteAllBlocksForUser kada nema blokova")
    void deleteAllBlocksForUser_noBlocks_shouldNotThrow() {
        when(blockRepository.deleteAllByUserId(99L)).thenReturn(0L);

        assertDoesNotThrow(() -> blockService.deleteAllBlocksForUser(99L));
    }

    // ==================== getUserIdByUsername() ====================

    @Nested
    @DisplayName("getUserIdByUsername() testovi")
    class GetUserIdTests {

        @Test
        @DisplayName("Vraca ID kada user-service dostupan")
        void getUserIdByUsername_shouldReturnId() {
            UserProfileDto dto = new UserProfileDto();
            dto.setId(42L);

            when(restTemplate.getForObject(contains("testuser"), eq(UserProfileDto.class)))
                    .thenReturn(dto);

            Long id = blockService.getUserIdByUsername("testuser");

            assertEquals(42L, id);
        }

        @Test
        @DisplayName("Baca RuntimeException kada user-service vraca null")
        void getUserIdByUsername_nullResponse_shouldThrow() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenReturn(null);

            assertThrows(RuntimeException.class,
                    () -> blockService.getUserIdByUsername("testuser"));
        }

        @Test
        @DisplayName("Baca RuntimeException kada user-service nedostupan")
        void getUserIdByUsername_serviceDown_shouldThrow() {
            when(restTemplate.getForObject(anyString(), eq(UserProfileDto.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            assertThrows(RuntimeException.class,
                    () -> blockService.getUserIdByUsername("testuser"));
        }
    }
}