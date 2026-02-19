package com.instagram.follow_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.instagram.follow_service.dto.FollowCountDto;
import com.instagram.follow_service.dto.FollowDto;
import com.instagram.follow_service.dto.FollowRequestDto;
import com.instagram.follow_service.dto.UserProfileDto;
import com.instagram.follow_service.entity.Follow;
import com.instagram.follow_service.entity.FollowRequest;
import com.instagram.follow_service.entity.FollowRequest.RequestStatus;
import com.instagram.follow_service.repository.FollowRepository;
import com.instagram.follow_service.repository.FollowRequestRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final RestTemplate restTemplate;

    public FollowService(
        FollowRepository followRepository,
        FollowRequestRepository followRequestRepository,
        RestTemplate restTemplate
    ) {
        this.followRepository = followRepository;
        this.followRequestRepository = followRequestRepository;
        this.restTemplate = restTemplate;
    }

    /*
    Запрати корисника.
    Ако је профил јаван — одмах се креира Follow.
    Ако је профил приватан — креира се FollowRequest.
    */
    @Transactional
    public Map<String, String> follow(Long followerId, Long followingId) {
        // Не може да прати сам себе
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Не можете пратити сами себе.");
        }

        // Провери да ли већ прати
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalStateException("Већ пратите овог корисника.");
        }

        // Провери да ли је профил приватан позивом user-service
        UserProfileDto targetUser = getUserProfile(followingId);

        if (Boolean.TRUE.equals(targetUser.getPrivateProfile())) {
            // Приватан профил — провери да ли већ постоји PENDING захтев
            if (followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                    followerId, followingId, RequestStatus.PENDING)) {
                throw new IllegalStateException("Захтев за праћење је већ послат.");
            }

            FollowRequest request = FollowRequest.builder()
                .senderId(followerId)
                .receiverId(followingId)
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

            followRequestRepository.save(request);
            return Map.of("message", "Захтев за праћење је послат.");
        } else {
            // Јаван профил — одмах креирај Follow
            Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .createdAt(LocalDateTime.now())
                .build();

            followRepository.save(follow);
            return Map.of("message", "Успешно сте запратили корисника.");
        }
    }

    /*
    Отпрати корисника.
    */
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
            .orElseThrow(() -> new RuntimeException("Не пратите овог корисника."));
        followRepository.delete(follow);
    }

    /*
    Уклони пратиоца (корисник уклања некога ко га прати).
    */
    @Transactional
    public void removeFollower(Long userId, Long followerId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, userId)
            .orElseThrow(() -> new RuntimeException("Овај корисник вас не прати."));
        followRepository.delete(follow);
    }

    /*
    Прихвати захтев за праћење.
    */
    @Transactional
    public void acceptRequest(Long requestId, Long currentUserId) {
        FollowRequest request = followRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Захтев није пронађен."));

        // Само прималац може да прихвати
        if (!request.getReceiverId().equals(currentUserId)) {
            throw new IllegalArgumentException("Немате право да прихватите овај захтев.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Захтев је већ обрађен.");
        }

        request.setStatus(RequestStatus.ACCEPTED);
        followRequestRepository.save(request);

        // Креирај Follow релацију
        Follow follow = Follow.builder()
            .followerId(request.getSenderId())
            .followingId(request.getReceiverId())
            .createdAt(LocalDateTime.now())
            .build();

        followRepository.save(follow);
    }

    /*
    Одбиј захтев за праћење.
    */
    @Transactional
    public void rejectRequest(Long requestId, Long currentUserId) {
        FollowRequest request = followRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Захтев није пронађен."));

        if (!request.getReceiverId().equals(currentUserId)) {
            throw new IllegalArgumentException("Немате право да одбијете овај захтев.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Захтев је већ обрађен.");
        }

        request.setStatus(RequestStatus.REJECTED);
        followRequestRepository.save(request);
    }

    /*
    Листа PENDING захтева за тренутног корисника.
    */
    public List<FollowRequestDto> getPendingRequests(Long userId) {
        return followRequestRepository.findByReceiverIdAndStatus(userId, RequestStatus.PENDING)
            .stream()
            .map(this::toRequestDto)
            .collect(Collectors.toList());
    }

    /*
    Листа пратилаца.
    */
    public List<FollowDto> getFollowers(Long userId) {
        return followRepository.findByFollowingId(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /*
    Листа профила које корисник прати.
    */
    public List<FollowDto> getFollowing(Long userId) {
        return followRepository.findByFollowerId(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /*
    Број пратилаца и праћених.
    */
    public FollowCountDto getFollowCount(Long userId) {
        return FollowCountDto.builder()
            .userId(userId)
            .followersCount(followRepository.countByFollowingId(userId))
            .followingCount(followRepository.countByFollowerId(userId))
            .build();
    }

    /*
    Да ли followerId прати followingId.
    */
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /*
    Позови user-service да добијеш профил корисника.
    */
    private UserProfileDto getUserProfile(Long userId) {
        try {
            return restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/id/" + userId,
                UserProfileDto.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Корисник није пронађен.");
        }
    }

    private FollowDto toDto(Follow follow) {
        return FollowDto.builder()
            .id(follow.getId())
            .followerId(follow.getFollowerId())
            .followingId(follow.getFollowingId())
            .createdAt(follow.getCreatedAt())
            .build();
    }

    private FollowRequestDto toRequestDto(FollowRequest request) {
        return FollowRequestDto.builder()
            .id(request.getId())
            .senderId(request.getSenderId())
            .receiverId(request.getReceiverId())
            .status(request.getStatus().name())
            .createdAt(request.getCreatedAt())
            .build();
    }
}