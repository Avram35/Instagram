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

import com.instagram.interactive_service.dto.CommentDto;
import com.instagram.interactive_service.dto.UserProfileDto;
import com.instagram.interactive_service.entity.Comment;
import com.instagram.interactive_service.repository.CommentRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final RestTemplate restTemplate;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public CommentService(CommentRepository commentRepository, RestTemplate restTemplate) {
        this.commentRepository = commentRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Dodaj komentar na objavu  .
     */
    public CommentDto addComment(Long userId, Long postId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Коментар не може бити празан.");
        }

        checkCanInteract(userId, postId);

        Comment comment = Comment.builder()
            .userId(userId)
            .postId(postId)
            .content(content.trim())
            .createdAt(LocalDateTime.now())
            .build();
        comment = commentRepository.save(comment);

        // Obavestava vlasnika objave da je dobio komentar na svoju objavu
        sendCommentNotification(userId, postId);

        return toDto(comment);
    }

    /**
     * Izmena komentara - samo vlasnik komentara.
     */
    public CommentDto updateComment(Long commentId, Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Коментар не може бити празан.");
        }

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Коментар није пронађен."));

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Немате дозволу за измену овог коментара.");
        }

        comment.setContent(content.trim());
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);

        return toDto(comment);
    }

    /**
     * Obrisi komentar - samo vlasnik komentara.
     */
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Коментар није пронађен."));

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Немате дозволу за брисање овог коментара.");
        }

        commentRepository.delete(comment);
    }

    /**
     * Prikazuje sve komentare za odredjenu objavu.
     */
    public List<CommentDto> getComments(Long postId, Long requesterId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        if (requesterId != null) {
            comments = comments.stream()
                .filter(c -> !isBlockedEither(requesterId, c.getUserId()))
                .collect(Collectors.toList());
        }

        return comments.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Broj komentara na objavi. Ne prikazuje blokirane komentare. Ako je korisnik ulogovan, prikazuje broj komentara koje je on lajkovao.
     */
    public long getCommentsCount(Long postId, Long requesterId) {
        if (requesterId == null) {
            return commentRepository.countByPostId(postId);
        }
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        return comments.stream()
            .filter(c -> !isBlockedEither(requesterId, c.getUserId()))
            .count();
    }

    /**
     * Obrisi sve komentare - interno, kada se objava brise.
     */
    @Transactional
    public void deleteAllByPostId(Long postId) {
        commentRepository.deleteByPostId(postId);
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
     * Posalji Comment obavestenje vlasniku objave da je dobio komentar.
     */
    private void sendCommentNotification(Long senderId, Long postId) {
        try {
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

            // Ne salji obavestenje samom sebi kada komentarises svoju objavu
            if (ownerId.equals(senderId)) return;

            Map<String, Object> body = new HashMap<>();
            body.put("recipientId", ownerId);
            body.put("senderId", senderId);
            body.put("type", "COMMENT");
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
            log.warn("Неуспешно слање COMMENT обавештења: {}", e.getMessage());
        }
    }

    private void checkCanInteract(Long userId, Long postId) {
        try {
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
                ResponseEntity<Map> followResponse = restTemplate.exchange(
                    "http://follow-service:8083/api/v1/follow/check-internal/" + userId + "/" + ownerId,
                    HttpMethod.GET,
                    entity,
                    Map.class
                );
                Map<String, Object> followCheck = followResponse.getBody();
                boolean isFollowing = followCheck != null && Boolean.TRUE.equals(followCheck.get("following"));
                if (!isFollowing) {
                    throw new IllegalArgumentException("Морате пратити овог корисника да бисте коментарисали.");
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

    private CommentDto toDto(Comment comment) {
        String username = null;
        String profilePictureUrl = null;
        try {
            UserProfileDto profile = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/id/" + comment.getUserId(),
                UserProfileDto.class
            );
            if (profile != null) {
                username = profile.getUsername();
                profilePictureUrl = profile.getProfilePictureUrl();
            }
        } catch (Exception e) {
            log.warn("Неуспешно преузимање корисника: {}", e.getMessage());
        }

        return CommentDto.builder()
            .id(comment.getId())
            .userId(comment.getUserId())
            .postId(comment.getPostId())
            .content(comment.getContent())
            .username(username)
            .profilePictureUrl(profilePictureUrl)
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }
}