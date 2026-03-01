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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.instagram.follow_service.dto.FollowCountDto;
import com.instagram.follow_service.dto.FollowRequestDto;
import com.instagram.follow_service.dto.UserProfileDto;
import com.instagram.follow_service.repository.FollowRepository;
import com.instagram.follow_service.service.FollowService;

@RestController
@RequestMapping("/api/v1/follow")
public class FollowController {

    private final FollowService followService;
    private final RestTemplate restTemplate;
    private final FollowRepository followRepository;

    public FollowController(FollowService followService, RestTemplate restTemplate, FollowRepository followRepository) {
        this.followService = followService;
        this.restTemplate = restTemplate;
        this.followRepository = followRepository;
    }

    /*
    Запрати корисника. Ако је приватан — шаље захтев.
    */
    @PostMapping("/{userId}")
    public ResponseEntity<Map<String, String>> follow(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = getCurrentUserId(currentUser.getUsername());
        Map<String, String> result = followService.follow(currentUserId, userId);
        return ResponseEntity.ok(result);
    }

    /*
    Отпрати корисника.
    */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> unfollow(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = getCurrentUserId(currentUser.getUsername());
        followService.unfollow(currentUserId, userId);
        return ResponseEntity.ok(Map.of("message", "Успешно сте отпратили корисника."));
    }

    /*
    Уклони пратиоца (корисник уклања некога ко га прати).
    */
    @DeleteMapping("/remove/{followerId}")
    public ResponseEntity<Map<String, String>> removeFollower(
        @PathVariable Long followerId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = getCurrentUserId(currentUser.getUsername());
        followService.removeFollower(currentUserId, followerId);
        return ResponseEntity.ok(Map.of("message", "Пратилац је уклоњен."));
    }

    /*
    Интерни ендпоинт који се позива из осталих сервиса.
    */
    @DeleteMapping("/internal/unfollow")
    public ResponseEntity<Void> internalUnfollow(
        @RequestParam Long followerId,
        @RequestParam Long followingId
    ) {
        try {
            followService.unfollow(followerId, followingId);
        } catch (Exception e) {
            // Ако не прати — игнориши
        }
        return ResponseEntity.noContent().build();
    }

    /*
    Прихвати захтев за праћење.
    */
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<Map<String, String>> acceptRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = getCurrentUserId(currentUser.getUsername());
        followService.acceptRequest(requestId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Захтев за праћење је прихваћен."));
    }

    /*
    Одбиј захтев за праћење.
    */
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<Map<String, String>> rejectRequest(
        @PathVariable Long requestId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = getCurrentUserId(currentUser.getUsername());
        followService.rejectRequest(requestId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Захтев за праћење је одбијен."));
    }

    /*
    Листа PENDING захтева за тренутног корисника.
    */
    @GetMapping("/requests/pending")
    public ResponseEntity<List<FollowRequestDto>> getPendingRequests(
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = getCurrentUserId(currentUser.getUsername());
        return ResponseEntity.ok(followService.getPendingRequests(currentUserId));
    }

    /*
    Листа пратилаца.
    */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<?> getFollowers(
        @PathVariable Long userId,
         @AuthenticationPrincipal UserDetails currentUser) {
        // Провери да ли је профил приватан
        if (!canViewFollowList(userId, currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Приватан профил."));
        }
         return ResponseEntity.ok(followService.getFollowers(userId));
        
    }

    /*
    Листа профила које корисник прати.
    */
    @GetMapping("/{userId}/following")
    public ResponseEntity<?> getFollowing(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        if (!canViewFollowList(userId, currentUser)) {
            return ResponseEntity.status(403).body(Map.of("error", "Приватан профил."));
        }
        return ResponseEntity.ok(followService.getFollowing(userId));
    }

    private boolean canViewFollowList(Long profileUserId, UserDetails currentUser) {
        // Провери да ли је профил приватан
        try {
            UserProfileDto profile = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/id/" + profileUserId,
                UserProfileDto.class
            );
            // Јавни профил — сви виде
            if (profile == null || profile.getPrivateProfile() == null || !profile.getPrivateProfile()) {
                return true;
            }
        } catch (Exception e) {
            return true; // Ако user-service није доступан, дозволи
        }

        // Приватан профил — мора бити улогован
        if (currentUser == null) {
            return false;
        }

        Long currentUserId = getCurrentUserId(currentUser.getUsername());

        // Власник види свој профил
        if (currentUserId.equals(profileUserId)) {
            return true;
        }

        // Пратилац види
        return followRepository.existsByFollowerIdAndFollowingId(currentUserId, profileUserId);
    }

    /*
    Број пратилаца и праћених.
    */
    @GetMapping("/{userId}/count")
    public ResponseEntity<FollowCountDto> getFollowCount(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowCount(userId));
    }

    /*
    Да ли тренутни корисник прати неког.
    */
    @GetMapping("/check/{userId}")
    public ResponseEntity<Map<String, Boolean>> isFollowing(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long currentUserId = getCurrentUserId(currentUser.getUsername());
        boolean following = followService.isFollowing(currentUserId, userId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    @GetMapping("/check-internal/{followerId}/{followingId}")
    public ResponseEntity<Map<String, Boolean>> checkInternal(
        @PathVariable Long followerId,
        @PathVariable Long followingId
    ) {
        boolean following = followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
        return ResponseEntity.ok(Map.of("following", following));
    }

    /*
    Помоћни метод — из username-а добијамо userId позивом user-service.
    */
    private Long getCurrentUserId(String username) {
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

    /*
    Интерни — прихвати све pending захтеве за корисника.
    */
    @PostMapping("/requests/accept-all/{userId}")
    public ResponseEntity<Void> acceptAllPending(@PathVariable Long userId) {
        followService.acceptAllPendingRequests(userId);
        return ResponseEntity.noContent().build();
    }

}