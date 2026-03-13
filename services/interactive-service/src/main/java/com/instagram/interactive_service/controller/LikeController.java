package com.instagram.interactive_service.controller;

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

import com.instagram.interactive_service.dto.LikeDto;
import com.instagram.interactive_service.service.LikeService;

@RestController
@RequestMapping("/api/v1/like")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    /**
     *Omogucava lajkovanje objave 
     * POST /api/v1/like/{postId}
     */
    @PostMapping("/{postId}")
    public ResponseEntity<?> likePost(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long userId = likeService.getUserIdByUsername(currentUser.getUsername());
        LikeDto like = likeService.likePost(userId, postId);
        return ResponseEntity.ok(like);
    }

    /**
     * Uogucava opciju odlajkovanja objave tj uklanjanja lajka sa te specificne objave
     * DELETE /api/v1/like/{postId}
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> unlikePost(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long userId = likeService.getUserIdByUsername(currentUser.getUsername());
        likeService.unlikePost(userId, postId);
        return ResponseEntity.ok(Map.of("message", "Лајк је уклоњен."));
    }

    /**
     *Proverava da li je korisnik lajkovao odredjenu objavu. Ako nije ulogovan, uvek vraća false.
     * GET /api/v1/like/check/{postId}
     */
    @GetMapping("/check/{postId}")
    public ResponseEntity<Map<String, Boolean>> hasLiked(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long userId = likeService.getUserIdByUsername(currentUser.getUsername());
        boolean liked = likeService.hasLiked(userId, postId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    /**
     *Prikazuje broj lajkova na objavi 
     * GET /api/v1/like/count/{postId}
     */
    @GetMapping("/count/{postId}")
    public ResponseEntity<Map<String, Long>> getLikesCount(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long requesterId = null;
        if (currentUser != null) {
            try { requesterId = likeService.getUserIdByUsername(currentUser.getUsername()); } catch (Exception ignored) {}
        }
        long count = likeService.getLikesCount(postId, requesterId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Lista svih krisnika koji su lajkovali odredjenu objavu 
     * GET /api/v1/like/{postId}/list
     */
    @GetMapping("/{postId}/list")
    public ResponseEntity<List<LikeDto>> getLikes(@PathVariable Long postId) {
        return ResponseEntity.ok(likeService.getLikes(postId));
    }

    /**
     * Interni - brisi sve lajkove za odredjenu objavu (kada se objava brise).
     * DELETE /api/v1/like/internal/post/{postId}
     */
    @DeleteMapping("/internal/post/{postId}")
    public ResponseEntity<Void> deleteAllByPost(@PathVariable Long postId) {
        likeService.deleteAllByPostId(postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/internal/user/{userId}")
    public ResponseEntity<Void> deleteAllByUser(@PathVariable Long userId) {
        likeService.deleteAllByUserId(userId);
        return ResponseEntity.noContent().build();
    }
    // DELETE /api/v1/like/internal/user/{userId} - Interni endpoint koji briše sve lajkove koje je korisnik postavio. 
    // Koristi se kada se korisnik obriše, da bi se obrisali i svi njegovi lajkovi.
}