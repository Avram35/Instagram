package com.instagram.blok_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.instagram.blok_service.dto.BlockDto;
import com.instagram.blok_service.service.BlockService;

@RestController
@RequestMapping("/api/v1/block")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Map<String, String>> block(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        blockService.block(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "Корисник је блокиран."));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> unblock(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        blockService.unblock(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "Корисник је одблокиран."));
    }

    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Boolean>> isBlocked(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        boolean blocked = blockService.isBlocked(currentUserId, userId);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }
    
    @GetMapping("/check-by/{userId}")
    public ResponseEntity<Map<String, Boolean>> isBlockedBy(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        boolean blocked = blockService.isBlocked(userId, currentUserId);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }

    @GetMapping("/internal/check-either/{userId1}/{userId2}")
    public ResponseEntity<Map<String, Boolean>> isBlockedEitherWay(
        @PathVariable Long userId1,
        @PathVariable Long userId2
    ) {
        boolean blocked = blockService.isBlockedEitherWay(userId1, userId2);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }

    @GetMapping("/list")
    public ResponseEntity<List<BlockDto>> getBlockedUsers(
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        return ResponseEntity.ok(blockService.getBlockedUsers(currentUserId));
    }
    // Poziva auth-service pri deleteAccount() — brise sve blokove gde je korisnik ucestvovao
    @DeleteMapping("/internal/user/{userId}")
    public ResponseEntity<Void> deleteAllBlocksForUser(@PathVariable Long userId) {
        blockService.deleteAllBlocksForUser(userId);
        return ResponseEntity.noContent().build();
    }
}
