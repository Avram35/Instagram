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
     * Lajkovanje objave 
     * // - svaki korisnik može da lajkuje objavu samo jednom, ako pokuša ponovo, lajka će se ukloniti (unlike)
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
     * Uklanjanje lajka 
     * DELETE /api/v1/like/{postId}
     */
    // - korisnik može da ukloni lajka sa objave, ali samo ako je prethodno lajkovao tu objavu
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
     * Da li je post lajkovan od strane trenutnog korisnika
     * // - svaki korisnik može da vidi da li je lajkovao objavu, ali ne može da vidi ko su ostali korisnici koji su lajkovali objavu
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
     * Broj lajkova na objavi
     * GET /api/v1/like/count/{postId}
     */
    // - svi korisnici mogu da vide broj lajkova, ali se računaju samo oni koji nisu uklonjeni (unliked)
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
     * Ko je sve lajkovao objavu
     * GET /api/v1/like/{postId}/list
     */
    // - svi korisnici mogu da vide broj lajkova, ali se ne prikazuju korisnici koji su uklonili lajka (unliked)
     
    @GetMapping("/{postId}/list")
    public ResponseEntity<List<LikeDto>> getLikes(@PathVariable Long postId) {
        return ResponseEntity.ok(likeService.getLikes(postId));
    }

    /**
     * Interni endpoint za brisanje svih lajkova na objavi (kada se objava briše)
     * DELETE /api/v1/like/internal/post/{postId}
     */
    // - Ovaj endpoint nije za javnu upotrebu, koristi se samo unutar sistema kada se briše objava, da bi se obrisali svi lajkovi vezani za tu objavu.
     
    @DeleteMapping("/internal/post/{postId}")
    public ResponseEntity<Void> deleteAllByPost(@PathVariable Long postId) {
        likeService.deleteAllByPostId(postId);
        return ResponseEntity.noContent().build();
    }
}