package com.instagram.feed_service.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.instagram.feed_service.dto.FeedPostDto;
import com.instagram.feed_service.dto.FollowDto;
import com.instagram.feed_service.dto.UserProfileDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FeedService {

    private final RestTemplate restTemplate;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public FeedService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<FeedPostDto> getFeed(String username, int page, int size) {
        Long userId = getUserIdByUsername(username);

        List<Long> followingIds = getFollowingIds(userId);

        if (followingIds.isEmpty()) {
            return Collections.emptyList();
        }

        //Filtriranje blokirane korisnike iz feed-a
        List<Long> filteredIds = followingIds.stream()
            .filter(followingId -> !isBlockedEither(userId, followingId))
            .collect(Collectors.toList());

        if (filteredIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<FeedPostDto> allPosts = new ArrayList<>();
        for (Long followingId : filteredIds) {
            List<FeedPostDto> posts = getPostsByUserId(followingId);
            allPosts.addAll(posts);
        }

        allPosts.sort(Comparator.comparing(FeedPostDto::getCreatedAt).reversed());

        int start = page * size;
        if (start >= allPosts.size()) {
            return Collections.emptyList();
        }
        int end = Math.min(start + size, allPosts.size());

        return allPosts.subList(start, end);
    }

    private Long getUserIdByUsername(String username) {
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
            throw new RuntimeException("Грешка при преузимању корисничких података: " + e.getMessage());
        }
    }

    private List<Long> getFollowingIds(Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<FollowDto>> response = restTemplate.exchange(
                "http://follow-service:8083/api/v1/follow/" + userId + "/following",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<FollowDto>>() {}
            );

            List<FollowDto> following = response.getBody();
            if (following == null) {
                return Collections.emptyList();
            }

            return following.stream()
                .map(FollowDto::getFollowingId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Follow сервис није доступан: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<FeedPostDto> getPostsByUserId(Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<FeedPostDto>> response = restTemplate.exchange(
                "http://post-service:8086/api/v1/post/internal/user/" + userId,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<FeedPostDto>>() {}
            );

            List<FeedPostDto> posts = response.getBody();
            return posts != null ? posts : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Post сервис није доступан за корисника {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    //Provera blokova pre prikazivanja u feedu
    private boolean isBlockedEither(Long userId1, Long userId2) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                "http://blok-service:8084/api/v1/block/internal/check-either/" + userId1 + "/" + userId2,
                HttpMethod.GET,
                entity,
                Map.class
            );
            return response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("blocked"));
        } catch (Exception e) {
            log.warn("Блок сервис недоступан: {}", e.getMessage());
            return false;
        }
    }
}