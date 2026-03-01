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

    /*
    Блокирај корисника.
    */
    @PostMapping("/{userId}")
    public ResponseEntity<Map<String, String>> block(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        blockService.block(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "Корисник је блокиран."));
    }

    /*
    Одблокирај корисника.
    */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> unblock(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        blockService.unblock(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "Корисник је одблокиран."));
    }

    /*
    Провери да ли је тренутни корисник блокирао неког.
    */
    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Boolean>> isBlocked(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        boolean blocked = blockService.isBlocked(currentUserId, userId);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }
    
    /*
    Провери да ли је неки корисник блокирао тренутног корисника.
    */
    @GetMapping("/check-by/{userId}")
    public ResponseEntity<Map<String, Boolean>> isBlockedBy(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        boolean blocked = blockService.isBlocked(userId, currentUserId);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }

    /*
    Провери да ли постоји блок у било ком смеру.
    Остали сервиси позивају овај ендпоинт.
    */
    @GetMapping("/check-either/{userId1}/{userId2}")
    public ResponseEntity<Map<String, Boolean>> isBlockedEitherWay(
        @PathVariable Long userId1,
        @PathVariable Long userId2
    ) {
        boolean blocked = blockService.isBlockedEitherWay(userId1, userId2);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }

    /*
    Листа блокираних корисника.
    */
    @GetMapping("/list")
    public ResponseEntity<List<BlockDto>> getBlockedUsers(
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = blockService.getUserIdByUsername(currentUser.getUsername());
        return ResponseEntity.ok(blockService.getBlockedUsers(currentUserId));
    }
}