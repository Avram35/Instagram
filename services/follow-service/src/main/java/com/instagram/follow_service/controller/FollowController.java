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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.instagram.follow_service.dto.FollowCountDto;
import com.instagram.follow_service.dto.FollowDto;
import com.instagram.follow_service.dto.FollowRequestDto;
import com.instagram.follow_service.dto.UserProfileDto;
import com.instagram.follow_service.service.FollowService;

@RestController
@RequestMapping("/api/v1/follow")
public class FollowController {

    private final FollowService followService;
    private final RestTemplate restTemplate;

    public FollowController(FollowService followService, RestTemplate restTemplate) {
        this.followService = followService;
        this.restTemplate = restTemplate;
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
    public ResponseEntity<List<FollowDto>> getFollowers(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowers(userId));
    }

    /*
    Листа профила које корисник прати.
    */
    @GetMapping("/{userId}/following")
    public ResponseEntity<List<FollowDto>> getFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getFollowing(userId));
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
}