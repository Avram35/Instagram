package com.instagram.blok_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.instagram.blok_service.dto.BlockDto;
import com.instagram.blok_service.dto.UserProfileDto;
import com.instagram.blok_service.entity.Block;
import com.instagram.blok_service.repository.BlockRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BlockService {

    private final BlockRepository blockRepository;
    private final RestTemplate restTemplate;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public BlockService(BlockRepository blockRepository, RestTemplate restTemplate) {
        this.blockRepository = blockRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public BlockDto block(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("Не можете блокирати сами себе.");
        }

        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new IllegalStateException("Корисник је већ блокиран.");
        }

        removeFollowIfExists(blockerId, blockedId);
        removeFollowIfExists(blockedId, blockerId);

        Block block = Block.builder()
            .blockerId(blockerId)
            .blockedId(blockedId)
            .createdAt(LocalDateTime.now())
            .build();

        Block saved = blockRepository.save(block);
        return toDto(saved);
    }

    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        Block block = blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
            .orElseThrow(() -> new RuntimeException("Корисник није блокиран."));
        blockRepository.delete(block);
    }

    public boolean isBlocked(Long blockerId, Long blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    public boolean isBlockedEitherWay(Long userId1, Long userId2) {
        return blockRepository.existsByBlockerIdAndBlockedId(userId1, userId2)
            || blockRepository.existsByBlockerIdAndBlockedId(userId2, userId1);
    }

    public List<BlockDto> getBlockedUsers(Long blockerId) {
        return blockRepository.findByBlockerId(blockerId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    // Poziva se pri brisanju naloga — brise sve gde je korisnik bloker/blokiran
    @Transactional
    public void deleteAllBlocksForUser(Long userId) {
        long deleted = blockRepository.deleteAllByUserId(userId);
        log.info("Обрисано {} блокова за корисника {}", deleted, userId);
    }

    private void removeFollowIfExists(Long followerId, Long followingId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                "http://follow-service:8083/api/v1/follow/internal/unfollow?followerId=" + followerId + "&followingId=" + followingId,
                HttpMethod.DELETE,
                entity,
                Void.class
            );
        } catch (Exception e) {
            log.warn("Неуспешно уклањање праћења између {} и {}: {}", 
                followerId, followingId, e.getMessage());
        }
    }

    public Long getUserIdByUsername(String username) {
        try {
            UserProfileDto profile = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/" + username,
                UserProfileDto.class
            );
            if (profile == null) {
                throw new RuntimeException("Корисник није пронађен.");
            }
            return profile.getId();
        } catch (Exception e) {
            throw new RuntimeException("Грешка при преузимању корисничких података.");
        }
    }

    private BlockDto toDto(Block block) {
        return BlockDto.builder()
            .id(block.getId())
            .blockerId(block.getBlockerId())
            .blockedId(block.getBlockedId())
            .createdAt(block.getCreatedAt())
            .build();
    }
}
