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

    // ==================== PRACENJE ====================

    @Transactional
    public Map<String, String> follow(Long followerId, Long followingId) {

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
            log.warn("Блок сервис није доступан: {}", e.getMessage());
        }

        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Не можете пратити сами себе.");
        }

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalStateException("Већ пратите овог корисника.");
        }

        UserProfileDto targetUser = getUserProfile(followingId);

        if (Boolean.TRUE.equals(targetUser.getPrivateProfile())) {
            if (followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
                    followerId, followingId, RequestStatus.PENDING)) {
                throw new IllegalStateException("Захтев за праћење је већ послат.");
            }

            followRequestRepository.findBySenderIdAndReceiverId(followerId, followingId).ifPresent(existing -> {
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

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        Optional<Follow> follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
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

    @Transactional
    public void removeFollower(Long userId, Long followerId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, userId)
            .orElseThrow(() -> new RuntimeException("Овај корисник вас не прати."));
        followRepository.delete(follow);

        followRequestRepository.findBySenderIdAndReceiverId(followerId, userId)
            .ifPresent(followRequestRepository::delete);

        notificationRepository.deleteBySenderIdAndRecipientId(followerId, userId);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        // Obrisi sve follow relacije gde je korisnik follower ILI following
        followRepository.deleteByFollowerIdOrFollowingId(userId, userId);

        // Obrisi sve follow zahteve gde je korisnik sender ILI receiver
        followRequestRepository.deleteBySenderIdOrReceiverId(userId, userId);

        // Obrisi sve notifikacije gde je korisnik recipient ILI sender
        notificationRepository.deleteByRecipientIdOrSenderId(userId, userId);

        log.info("Обрисани сви follow подаци за корисника {}", userId);
    }

    // ==================== ZAHTEVI ====================

    @Transactional
    public void acceptRequest(Long requestId, Long currentUserId) {
        FollowRequest request = followRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Захтев није пронађен."));

        if (!request.getReceiverId().equals(currentUserId)) {
            throw new IllegalArgumentException("Немате право да прихватите овај захтев.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Захтев је већ обрађен.");
        }

        request.setStatus(RequestStatus.ACCEPTED);
        followRequestRepository.save(request);

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

    @Transactional
    public void acceptAllPendingRequests(Long userId) {
        List<FollowRequest> pending = followRequestRepository.findByReceiverIdAndStatus(userId, RequestStatus.PENDING);
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

    public List<FollowRequestDto> getPendingRequests(Long userId) {
        return followRequestRepository.findByReceiverIdAndStatus(userId, RequestStatus.PENDING)
            .stream()
            .map(this::toRequestDto)
            .collect(Collectors.toList());
    }

    public boolean hasPendingRequest(Long senderId, Long receiverId) {
        return followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
            senderId, receiverId, RequestStatus.PENDING
        );
    }

    // ==================== LISTE I BROJEVI ====================

    public List<FollowDto> getFollowers(Long userId) {
        return followRepository.findByFollowingId(userId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<FollowDto> getFollowing(Long userId) {
        return followRepository.findByFollowerId(userId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    public FollowCountDto getFollowCount(Long userId) {
        return FollowCountDto.builder()
            .userId(userId)
            .followersCount(followRepository.countByFollowingId(userId))
            .followingCount(followRepository.countByFollowerId(userId))
            .build();
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public boolean checkInternalFollow(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public Map<String, Boolean> getFollowStatus(Long followerId, Long followingId) {
        boolean following = followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
        boolean pending = followRequestRepository.existsBySenderIdAndReceiverIdAndStatus(
            followerId, followingId, RequestStatus.PENDING
        );
        return Map.of("following", following, "pending", pending);
    }

    // ==================== NOTIFIKACIJE ====================

    public void createInternalNotification(Map<String, Object> body) {
        try {
            Long recipientId = Long.valueOf(body.get("recipientId").toString());
            Long senderId = Long.valueOf(body.get("senderId").toString());
            String type = body.get("type").toString();
            Long postId = body.get("postId") != null ? Long.valueOf(body.get("postId").toString()) : null;

            if (recipientId.equals(senderId)) return;

            notificationRepository.save(Notification.builder()
                .recipientId(recipientId)
                .senderId(senderId)
                .type(type)
                .postId(postId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());
        } catch (Exception e) {
            log.warn("Грешка при креирању обавештења: {}", e.getMessage());
        }
    }

    public List<NotificationDto> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository
            .findByRecipientIdOrderByCreatedAtDesc(userId);

        return notifications.stream().map(n -> {
            String username = null;
            String pic = null;
            try {
                UserProfileDto sender = restTemplate.getForObject(
                    "http://user-service:8082/api/v1/user/id/" + n.getSenderId(),
                    UserProfileDto.class
                );
                if (sender != null) {
                    username = sender.getUsername();
                    pic = sender.getProfilePictureUrl();
                }
            } catch (Exception e) {
                log.warn("Неуспешно преузимање корисника {}: {}", n.getSenderId(), e.getMessage());
            }

            return NotificationDto.builder()
                .id(n.getId())
                .senderId(n.getSenderId())
                .senderUsername(username)
                .senderProfilePicture(pic)
                .type(n.getType())
                .postId(n.getPostId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
        }).collect(Collectors.toList());
    }

    public boolean canViewFollowList(Long profileUserId, Long currentUserId) {
        try {
            UserProfileDto profile = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/id/" + profileUserId,
                UserProfileDto.class
            );
            if (profile == null || profile.getPrivateProfile() == null || !profile.getPrivateProfile()) {
                return true;
            }
        } catch (Exception e) {

            log.warn("User-service недоступан за проверу приватности: {}", e.getMessage());
            return false;
        }

        if (currentUserId == null) return false;
        if (currentUserId.equals(profileUserId)) return true;
        return followRepository.existsByFollowerIdAndFollowingId(currentUserId, profileUserId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByRecipientId(userId);
    }

    // ==================== POMOCNE METODE ====================

    public Long getUserIdByUsername(String username) {
        try {
            UserProfileDto profile = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/" + username,
                UserProfileDto.class
            );
            if (profile == null) throw new RuntimeException("Корисник није пронађен.");
            return profile.getId();
        } catch (Exception e) {
            throw new RuntimeException("Грешка при преузимању корисничких података.");
        }
    }

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