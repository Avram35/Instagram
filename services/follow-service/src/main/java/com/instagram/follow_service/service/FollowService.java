package com.instagram.follow_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.instagram.follow_service.dto.FollowCountDto;
import com.instagram.follow_service.dto.FollowDto;
import com.instagram.follow_service.dto.FollowRequestDto;
import com.instagram.follow_service.dto.NotificationDto;
import com.instagram.follow_service.dto.UserProfileDto;
import com.instagram.follow_service.entity.Follow;
import com.instagram.follow_service.entity.FollowRequest;
import com.instagram.follow_service.entity.FollowRequest.RequestStatus;
import com.instagram.follow_service.entity.Notification;
import com.instagram.follow_service.repository.FollowRepository;
import com.instagram.follow_service.repository.FollowRequestRepository;
import com.instagram.follow_service.repository.NotificationRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public FollowService(
        FollowRepository followRepository,
        FollowRequestRepository followRequestRepository,
        NotificationRepository notificationRepository,
        RestTemplate restTemplate
    ) {
        this.followRepository = followRepository;
        this.followRequestRepository = followRequestRepository;
        this.notificationRepository = notificationRepository;
        this.restTemplate = restTemplate;
    }

    /*
    Запрати корисника.
    Ако је профил јаван — одмах се креира Follow.
    Ако је профил приватан — креира се FollowRequest.
    */
    @Transactional
    public Map<String, String> follow(Long followerId, Long followingId) {

        // Провери да ли постоји блок у било ком смеру
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                "http://blok-service:8084/api/v1/block/internal/check-either/" + followerId + "/" + followingId,
                HttpMethod.GET,
                entity,
                Map.class
            );

            Boolean blocked = (Boolean) response.getBody().get("blocked");

            if (Boolean.TRUE.equals(blocked)) {
                throw new IllegalStateException("Не можете запратити овог корисника.");
            }

        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // Block сервис није доступан — дозволи праћење
            log.warn("Блок сервис није доступан: {}", e.getMessage());
        }

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

            followRequestRepository.findBySenderIdAndReceiverId(followerId, followingId)
                .ifPresent(existing -> {
                    followRequestRepository.delete(existing);
                    followRequestRepository.flush();
                });

            FollowRequest request = FollowRequest.builder()
                .senderId(followerId)
                .receiverId(followingId)
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

            followRequestRepository.save(request);

            notificationRepository.save(Notification.builder()
                .recipientId(followingId)
                .senderId(followerId)
                .type("FOLLOW_REQUEST")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());

            return Map.of("message", "Захтев за праћење је послат.");

        } else {

            // Јаван профил — одмах креирај Follow
            Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .createdAt(LocalDateTime.now())
                .build();

            followRepository.save(follow);

            notificationRepository.save(Notification.builder()
                .recipientId(followingId)
                .senderId(followerId)
                .type("FOLLOW")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());

            return Map.of("message", "Успешно сте запратили корисника.");
        }
    }

    /*
    Отпрати корисника.
    */
    @Transactional
    public void unfollow(Long followerId, Long followingId) {

        Optional<Follow> follow =
            followRepository.findByFollowerIdAndFollowingId(followerId, followingId);

        if (follow.isPresent()) {
            followRepository.delete(follow.get());
            notificationRepository.deleteBySenderIdAndRecipientId(followerId, followingId);
            return;
        }

        followRequestRepository.findBySenderIdAndReceiverId(followerId, followingId)
            .ifPresent(request -> {
                followRequestRepository.delete(request);
                notificationRepository.deleteBySenderIdAndRecipientId(followerId, followingId);
            });
    }

    /*
    Уклони пратиоца (корисник уклања некога ко га прати).
    */
    @Transactional
    public void removeFollower(Long userId, Long followerId) {

        Follow follow = followRepository
            .findByFollowerIdAndFollowingId(followerId, userId)
            .orElseThrow(() -> new RuntimeException("Овај корисник вас не прати."));

        followRepository.delete(follow);

        followRequestRepository.findBySenderIdAndReceiverId(followerId, userId)
            .ifPresent(followRequestRepository::delete);

        notificationRepository.deleteBySenderIdAndRecipientId(followerId, userId);
    }

    /*
    Интерни — обриши све follow податке корисника (кад се налог брише).
    */
    @Transactional
    public void deleteAllByUserId(Long userId) {

        followRepository.deleteByFollowerIdOrFollowingId(userId, userId);
        followRequestRepository.deleteBySenderIdOrReceiverId(userId, userId);
        notificationRepository.deleteByRecipientIdOrSenderId(userId, userId);

        log.info("Обрисани сви follow подаци за корисника {}", userId);
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

        LocalDateTime originalTime = request.getCreatedAt();

        notificationRepository.deleteBySenderIdAndRecipientId(
            request.getSenderId(), request.getReceiverId()
        );

        notificationRepository.save(Notification.builder()
            .recipientId(request.getReceiverId())
            .senderId(request.getSenderId())
            .type("FOLLOW")
            .read(false)
            .createdAt(originalTime)
            .build());
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

        request.setStatus(RequestStatus.REJECTED);
        followRequestRepository.save(request);

        notificationRepository.deleteBySenderIdAndRecipientId(
            request.getSenderId(), request.getReceiverId()
        );
    }

    /*
    Интерни — прихвати све pending захтеве за корисника.
    */
    @Transactional
    public void acceptAllPendingRequests(Long userId) {

        List<FollowRequest> pending =
            followRequestRepository.findByReceiverIdAndStatus(userId, RequestStatus.PENDING);

        for (FollowRequest req : pending) {

            req.setStatus(RequestStatus.ACCEPTED);
            followRequestRepository.save(req);

            Follow follow = Follow.builder()
                .followerId(req.getSenderId())
                .followingId(req.getReceiverId())
                .createdAt(LocalDateTime.now())
                .build();

            followRepository.save(follow);

            LocalDateTime originalTime = req.getCreatedAt();

            notificationRepository.deleteBySenderIdAndRecipientId(
                req.getSenderId(), req.getReceiverId()
            );

            notificationRepository.save(Notification.builder()
                .recipientId(req.getReceiverId())
                .senderId(req.getSenderId())
                .type("FOLLOW")
                .read(false)
                .createdAt(originalTime)
                .build());
        }
    }

    /*
    Листа PENDING захтева за тренутног корисника.
    */
    public List<FollowRequestDto> getPendingRequests(Long userId) {

        return followRequestRepository
            .findByReceiverIdAndStatus(userId, RequestStatus.PENDING)
            .stream()
            .map(this::toRequestDto)
            .collect(Collectors.toList());
    }

    /*
    Листа пратилаца.
    */
    public List<FollowDto> getFollowers(Long userId) {

        return followRepository
            .findByFollowingId(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /*
    Листа профила које корисник прати.
    */
    public List<FollowDto> getFollowing(Long userId) {

        return followRepository
            .findByFollowerId(userId)
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

    /*
    Конверзија Follow ентитета у DTO.
    */
    private FollowDto toDto(Follow follow) {

        return FollowDto.builder()
            .id(follow.getId())
            .followerId(follow.getFollowerId())
            .followingId(follow.getFollowingId())
            .createdAt(follow.getCreatedAt())
            .build();
    }

    /*
    Конверзија FollowRequest ентитета у DTO.
    */
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