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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.instagram.interactive_service.dto.CommentDto;
import com.instagram.interactive_service.dto.CommentRequest;
import com.instagram.interactive_service.service.CommentService;

@RestController
@RequestMapping("/api/v1/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Komentarisanje objave
     * POST /api/v1/comment/{postId}
     */
    //    @PostMapping("/{postId}")
    public ResponseEntity<?> addComment(
        @PathVariable Long postId,
        @RequestBody CommentRequest request,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long userId = commentService.getUserIdByUsername(currentUser.getUsername());
        CommentDto comment = commentService.addComment(userId, postId, request.getContent());
        return ResponseEntity.ok(comment);
    
    }

    /**
     * Izmena komentara // samo autor komentara može da menja     * PUT /api/v1/comment/{commentId}
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
        @PathVariable Long commentId,
        @RequestBody CommentRequest request,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long userId = commentService.getUserIdByUsername(currentUser.getUsername());
        CommentDto comment = commentService.updateComment(commentId, userId, request.getContent());
        return ResponseEntity.ok(comment);
    }

    /**
     * Brisanje komentara 
     * - samo autor komentara ili vlasnik objave mogu da brišu
     *      * DELETE /api/v1/comment/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
        @PathVariable Long commentId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long userId = commentService.getUserIdByUsername(currentUser.getUsername());
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok(Map.of("message", "Коментар је обрисан."));
    }

    /**
     * Komentari na objavi // svi korisnici mogu da vide komentare, ali se prikazuju samo oni koji nisu obrisani    
     * GET /api/v1/comment/{postId}/list
     */
    @GetMapping("/{postId}/list")
    public ResponseEntity<List<CommentDto>> getComments(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long requesterId = null;
        if (currentUser != null) {
            try { requesterId = commentService.getUserIdByUsername(currentUser.getUsername()); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(commentService.getComments(postId, requesterId));
    }

    /**
     * Broj komentara na objavi
     * // - svi korisnici mogu da vide broj komentara, ali se računaju samo oni koji nisu obrisani
     * GET /api/v1/comment/count/{postId}
     */
    @GetMapping("/count/{postId}")
    public ResponseEntity<Map<String, Long>> getCommentsCount(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long requesterId = null;
        if (currentUser != null) {
            try { requesterId = commentService.getUserIdByUsername(currentUser.getUsername()); } catch (Exception ignored) {}
        }
        long count = commentService.getCommentsCount(postId, requesterId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Interni endpoint za brisanje svih komentara na objavi (kada se objava briše)
     * DELETE /api/v1/comment/internal/post/{postId}
     // - Ovaj endpoint nije za javnu upotrebu, koristi se samo unutar sistema kada se briše objava, da bi se obrisali svi komentari vezani za tu objavu.
     */
    @DeleteMapping("/internal/post/{postId}")
    public ResponseEntity<Void> deleteAllByPost(@PathVariable Long postId) {
        commentService.deleteAllByPostId(postId);
        return ResponseEntity.noContent().build();
    }
}