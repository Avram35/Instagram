package com.instagram.interactive_service.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    @Value("${internal.api.key}")
    private String internalApiKey;

    public LikeService(LikeRepository likeRepository, RestTemplate restTemplate) {
        this.likeRepository = likeRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Lajkuj objavu 
     */
    public LikeDto likePost(Long userId, Long postId) {
        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {
            throw new IllegalStateException("Већ сте означили да вам се свиђа ова објава.");
        }

        // Proveri da li korisnik moze da ima interakciju sa objavom 
        checkCanInteract(userId, postId);

        Like like = Like.builder()
            .userId(userId)
            .postId(postId)
            .createdAt(LocalDateTime.now())
            .build();
        like = likeRepository.save(like);

        // Posalji notifikaciju vlasniku objave 
        sendLikeNotification(userId, postId);

        return toDto(like);
    }

    /**
     * Ukloni lajk.
     */
    public void unlikePost(Long userId, Long postId) {
        Like like = likeRepository.findByUserIdAndPostId(userId, postId)
            .orElseThrow(() -> new RuntimeException("Нисте означили да вам се свиђа ова објава."));
        likeRepository.delete(like);
    }

    /**
     * Provera da li je korisnik lajkovao objavu .
     */
    public boolean hasLiked(Long userId, Long postId) {
        return likeRepository.existsByUserIdAndPostId(userId, postId);
    }

    /**
     * Broj lajkova na objavi - ne ubrajaj blokirane korisnike.
     */
    public long getLikesCount(Long postId, Long requesterId) {
        if (requesterId == null) {
            return likeRepository.countByPostId(postId);
        }
        // Filtiriraj blokirane korisnike
        List<Like> likes = likeRepository.findByPostId(postId);
        return likes.stream()
            .filter(l -> !isBlockedEither(requesterId, l.getUserId()))
            .count();
    }

    /**
     * Lista korisnika koji su lajkovali objavu. Ne prikazuje blokirane korisnike.
     */
    public List<LikeDto> getLikes(Long postId) {
        return likeRepository.findByPostId(postId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Obrisi sve lajkove za odredjenu objavu - interno, kada se objava brise.
     */
    @Transactional
    public void deleteAllByPostId(Long postId) {
        likeRepository.deleteByPostId(postId);
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

    /**
     * Posalji LIKE obavestenje vlasniku objave.
     */
    private void sendLikeNotification(Long senderId, Long postId) {
        try {
            // Preuzmi post → userId vlasnika
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> postResponse = restTemplate.exchange(
                "http://post-service:8086/api/v1/post/internal/" + postId,
                HttpMethod.GET,
                entity,
                Map.class
            );
            Map<String, Object> post = postResponse.getBody();
            if (post == null) return;

            Long ownerId = Long.valueOf(post.get("userId").toString());

            // Ne salji obavestenje samom sebi kada lajkujes svoju objavu
            if (ownerId.equals(senderId)) return;

            Map<String, Object> body = new HashMap<>();
            body.put("recipientId", ownerId);
            body.put("senderId", senderId);
            body.put("type", "LIKE");
            body.put("postId", postId);

            HttpHeaders notifHeaders = new HttpHeaders();
            notifHeaders.set("X-Internal-Api-Key", internalApiKey);
            notifHeaders.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> notifEntity = new HttpEntity<>(body, notifHeaders);

            restTemplate.postForEntity(
                "http://follow-service:8083/api/v1/follow/notifications/internal",
                notifEntity,
                Void.class
            );
        } catch (Exception e) {
            log.warn("Неуспешно слање LIKE обавештења: {}", e.getMessage());
        }
    }

    private void checkCanInteract(Long userId, Long postId) {
        try {
            // Preuzmi post → userId vlasnika
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> postResponse = restTemplate.exchange(
                "http://post-service:8086/api/v1/post/internal/" + postId,
                HttpMethod.GET,
                entity,
                Map.class
            );
            Map<String, Object> post = postResponse.getBody();
            if (post == null) throw new RuntimeException("Објава није пронађена.");

            Long ownerId = Long.valueOf(post.get("userId").toString());

            // Ako je vlasnik, dozvoljava 
            if (ownerId.equals(userId)) return;

            // Provera blokade - ako je bilo koja strana blokirala drugu, ne dozvoljava interakciju
            if (isBlockedEither(userId, ownerId)) {
                throw new IllegalArgumentException("Не можете интераговати са овом објавом.");
            }

            // Provera da li je profil privatan 
            UserProfileDto owner = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/id/" + ownerId,
                UserProfileDto.class
            );
            if (owner != null && Boolean.TRUE.equals(owner.getPrivateProfile())) {
                // Proveri da li prati 
                @SuppressWarnings("unchecked")
                ResponseEntity<Map> followResponse = restTemplate.exchange(
                    "http://follow-service:8083/api/v1/follow/check-internal/" + userId + "/" + ownerId,
                    HttpMethod.GET,
                    entity,
                    Map.class
                );
                Map<String, Object> followCheck = followResponse.getBody();
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
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.exchange(
                "http://blok-service:8084/api/v1/block/check-either/" + userId1 + "/" + userId2,
                HttpMethod.GET,
                entity,
                Map.class
            );
            Map<String, Object> res = response.getBody();
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