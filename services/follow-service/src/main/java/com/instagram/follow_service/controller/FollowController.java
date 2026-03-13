package com.instagram.follow_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.instagram.follow_service.dto.FollowCountDto;
import com.instagram.follow_service.dto.FollowDto;
import com.instagram.follow_service.dto.FollowRequestDto;
import com.instagram.follow_service.dto.NotificationDto;
import com.instagram.follow_service.service.FollowService;

// ===== ISPRAVKA: Kontroler koristi SAMO FollowService =====
// Pre: injektovao RestTemplate, FollowRepository, FollowRequestRepository, NotificationRepository
// Posle: samo FollowService — sva logika je u servisu

@RestController
@RequestMapping("/api/v1/follow")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    // ==================== PRACENJE ====================

    @PostMapping("/{userId}")
    public ResponseEntity<Map<String, String>> follow(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        return ResponseEntity.ok(followService.follow(currentUserId, userId));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> unfollow(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        followService.unfollow(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "Успешно сте отпратили корисника."));
    }

    @DeleteMapping("/remove/{followerId}")
    public ResponseEntity<Map<String, String>> removeFollower(
        @PathVariable Long followerId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        followService.removeFollower(currentUserId, followerId);
        return ResponseEntity.ok(Map.of("message", "Пратилац је уклоњен."));
    }

    // ==================== INTERNI ENDPOINTI ====================
    // Zasticeni InternalApiKeyFilter-om (X-Internal-Api-Key header)

    @DeleteMapping("/internal/unfollow")
    public ResponseEntity<Void> internalUnfollow(
        @RequestParam Long followerId,
        @RequestParam Long followingId
    ) {
        try {
            followService.unfollow(followerId, followingId);
        } catch (Exception e) { /* Ignorisi ako ne prati */ }
        return ResponseEntity.noContent().build();
    }

    /**
     * Интерни — обриши све follow податке корисника (кад се налог брише).
     * DELETE /api/v1/follow/internal/user/{userId}
     */
    @DeleteMapping("/internal/user/{userId}")
    public ResponseEntity<Void> deleteAllByUser(@PathVariable Long userId) {
        followService.deleteAllByUserId(userId);
        return ResponseEntity.noContent().build();
    }
    /**
     * Интерни — листа ID-jева које корисник прати (за feed-service).
     * GET /api/v1/follow/internal/{userId}/following
     */
    @GetMapping("/internal/{userId}/following")
    public ResponseEntity<List<FollowDto>> getFollowingInternal(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowing(userId));
    }

    @PostMapping("/notifications/internal")
    public ResponseEntity<Void> createInternalNotification(@RequestBody Map<String, Object> body) {
        followService.createInternalNotification(body);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/accept-all/{userId}")
    public ResponseEntity<Void> acceptAllPending(@PathVariable Long userId) {
        followService.acceptAllPendingRequests(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check-internal/{followerId}/{followingId}")
    public ResponseEntity<Map<String, Boolean>> checkInternal(
        @PathVariable Long followerId,
        @PathVariable Long followingId
    ) {
        boolean following = followService.checkInternalFollow(followerId, followingId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    // ==================== ZAHTEVI ZA PRACENJE ====================

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<Map<String, String>> acceptRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        followService.acceptRequest(requestId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Захтев за праћење је прихваћен."));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<Map<String, String>> rejectRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        followService.rejectRequest(requestId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Захтев за праћење је одбијен."));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<FollowRequestDto>> getPendingRequests(
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        return ResponseEntity.ok(followService.getPendingRequests(currentUserId));
    }

    @GetMapping("/requests/check/{userId}")
    public ResponseEntity<Map<String, Boolean>> hasPendingRequest(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        boolean pending = followService.hasPendingRequest(currentUserId, userId);
        return ResponseEntity.ok(Map.of("pending", pending));
    }

    // ==================== LISTE, BROJEVI, STATUS ====================

    @GetMapping("/{userId}/followers")
    public ResponseEntity<?> getFollowers(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = currentUser != null
            ? followService.getUserIdByUsername(currentUser.getUsername())
            : null;

        if (!followService.canViewFollowList(userId, currentUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Приватан профил."));
        }
        return ResponseEntity.ok(followService.getFollowers(userId));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<?> getFollowing(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = currentUser != null
            ? followService.getUserIdByUsername(currentUser.getUsername())
            : null;

        if (!followService.canViewFollowList(userId, currentUserId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Приватан профил."));
        }
        return ResponseEntity.ok(followService.getFollowing(userId));
    }

    @GetMapping("/{userId}/count")
    public ResponseEntity<FollowCountDto> getFollowCount(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowCount(userId));
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Boolean>> getFollowStatus(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        return ResponseEntity.ok(followService.getFollowStatus(currentUserId, userId));
    }

    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Boolean>> isFollowing(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        boolean following = followService.isFollowing(currentUserId, userId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    // ==================== NOTIFIKACIJE ====================

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        return ResponseEntity.ok(followService.getNotifications(currentUserId));
    }
    
    @PutMapping("/notifications/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = followService.getUserIdByUsername(currentUser.getUsername());
        followService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(Map.of("message", "Сва обавештења означена као прочитана."));
    }
}