package com.instagram.interactive_service.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.instagram.interactive_service.dto.LikeDto;
import com.instagram.interactive_service.dto.UserProfileDto;
import com.instagram.interactive_service.entity.Like;
import com.instagram.interactive_service.repository.LikeRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final RestTemplate restTemplate;

    public LikeService(LikeRepository likeRepository, RestTemplate restTemplate) {
        this.likeRepository = likeRepository;
        this.restTemplate = restTemplate;
    }

    /**
     *Lajkovanje objave. Proveri da li korisnik moze lajkovati (nije blokiran, i ako je profil privatan, da prati vlasnika)
     */
    public LikeDto likePost(Long userId, Long postId) {
        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new IllegalStateException("Већ сте означили да вам се свиђа ова објава.");
        }

        checkCanInteract(userId, postId);

        Like like = Like.builder()
            .userId(userId)
            .postId(postId)
            .createdAt(LocalDateTime.now())
            .build();
        like = likeRepository.save(like);

        // Notifikacija vlasniku objave o novom lajku
        sendLikeNotification(userId, postId);

        return toDto(like);
    }

    /**
     * Uklanjanje lajka
     */
    public void unlikePost(Long userId, Long postId) {
        Like like = likeRepository.findByUserIdAndPostId(userId, postId)
            .orElseThrow(() -> new RuntimeException("Нисте означили да вам се свиђа ова објава."));
        likeRepository.delete(like);
    }

    /**
     * Da li je post lajkovan od strane trenutnog korisnika
     */
    public boolean hasLiked(Long userId, Long postId) {
        return likeRepository.existsByUserIdAndPostId(userId, postId);
    }

    /**
     * Broj lajkova na objavi. Iskljuci blokirane
     */
    public long getLikesCount(Long postId, Long requesterId) {
        if (requesterId == null) {
            return likeRepository.countByPostId(postId);
        }

        List<Like> likes = likeRepository.findByPostId(postId);
        return likes.stream()
            .filter(l -> !isBlockedEither(requesterId, l.getUserId()))
            .count();
    }

    /**
     * Lista svih lajkova na objavi
     */
    public List<LikeDto> getLikes(Long postId) {
        return likeRepository.findByPostId(postId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Obrisi  sve lajkove na objavi (kada se objava brise)
     */
    @Transactional
    public void deleteAllByPostId(Long postId) {
        likeRepository.deleteByPostId(postId);
    }

    // pomocne metode

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

    /**
     * Posalji notifikaciju vlasniku objave da je neko lajkovao njegovu objavu
     */
    private void sendLikeNotification(Long senderId, Long postId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> post = restTemplate.getForObject(
                "http://post-service:8086/api/v1/post/internal/" + postId,
                Map.class
            );
            if (post == null) return;

            Long ownerId = Long.valueOf(post.get("userId").toString());

            if (ownerId.equals(senderId)) return;

            Map<String, Object> body = new HashMap<>();
            body.put("recipientId", ownerId);
            body.put("senderId", senderId);
            body.put("type", "LIKE");
            body.put("postId", postId);

            restTemplate.postForObject(
                "http://follow-service:8083/api/v1/follow/notifications/internal",
                body,
                Void.class
            );
        } catch (Exception e) {
            log.warn("Неуспешно слање LIKE обавештења: {}", e.getMessage());
        }
    }

    // Proveri da li korisnik moze lajkovati (nije blokiran, i ako je profil privatan, da prati vlasnika)
    private void checkCanInteract(Long userId, Long postId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> post = restTemplate.getForObject(
                "http://post-service:8086/api/v1/post/internal/" + postId,
                Map.class
            );
            if (post == null) throw new RuntimeException("Објава није пронађена.");

            Long ownerId = Long.valueOf(post.get("userId").toString());

            if (ownerId.equals(userId)) return;

            if (isBlockedEither(userId, ownerId)) {
                throw new IllegalArgumentException("Не можете интераговати са овом објавом.");
            }

            UserProfileDto owner = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/id/" + ownerId,
                UserProfileDto.class
            );
            if (owner != null && Boolean.TRUE.equals(owner.getPrivateProfile())) {
                @SuppressWarnings("unchecked")
                Map<String, Object> followCheck = restTemplate.getForObject(
                    "http://follow-service:8083/api/v1/follow/check-internal/" + userId + "/" + ownerId,
                    Map.class
                );
                boolean isFollowing = followCheck != null && Boolean.TRUE.equals(followCheck.get("following"));
                if (!isFollowing) {
                    throw new IllegalArgumentException("Морате пратити овог корисника да бисте интераговали.");
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Грешка при провери приступа: {}", e.getMessage());
        }
    }

    private boolean isBlockedEither(Long userId1, Long userId2) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                "http://blok-service:8084/api/v1/block/check-either/" + userId1 + "/" + userId2,
                Map.class
            );
            return res != null && Boolean.TRUE.equals(res.get("blocked"));
        } catch (Exception e) {
            return false;
        }
    }

    private LikeDto toDto(Like like) {
        String username = null;
        String profilePictureUrl = null;
        try {
            UserProfileDto profile = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/id/" + like.getUserId(),
                UserProfileDto.class
            );
            if (profile != null) {
                username = profile.getUsername();
                profilePictureUrl = profile.getProfilePictureUrl();
            }
        } catch (Exception e) {
            log.warn("Неуспешно преузимање корисника: {}", e.getMessage());
        }

        return LikeDto.builder()
            .id(like.getId())
            .userId(like.getUserId())
            .postId(like.getPostId())
            .username(username)
            .profilePictureUrl(profilePictureUrl)
            .createdAt(like.getCreatedAt())
            .build();
    }
}