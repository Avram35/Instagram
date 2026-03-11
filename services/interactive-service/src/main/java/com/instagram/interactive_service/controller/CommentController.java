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
     * POST /api/v1/comment/{postId}
     */
    // Dodaje komentare na objave 
        @PostMapping("/{postId}")
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
     * Dozvoljava korisniku da izmeni svoj komentar.
     * PUT /api/v1/comment/{commentId}
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
     * Dozvoljava korisniku da obriše svoj komentar.
     * DELETE /api/v1/comment/{commentId}
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
     Prikazuje sve komentare za oredjenu objavu 
     Ako je korisnik ulogovan, prikazuje i informaciju da li je lajkovao svaki komentar.
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
     Broj komentara za odredjenu objavu. Ako je korisnik ulogovan, prikazuje broj komentara koje je on lajkovao.
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
     * Interni - brisi sve komentare za odredjenu objavu. Koristi se kada se objava obrise, da bi se obrisali i svi komentari vezani za tu objavu.
     * DELETE /api/v1/comment/internal/post/{postId}
     */
    @DeleteMapping("/internal/post/{postId}")
    public ResponseEntity<Void> deleteAllByPost(@PathVariable Long postId) {
        commentService.deleteAllByPostId(postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/internal/user/{userId}")
    public ResponseEntity<Void> deleteAllByUser(@PathVariable Long userId) {
        commentService.deleteAllByUserId(userId);
        return ResponseEntity.noContent().build();
    } 
    // DELETE /api/v1/comment/internal/user/{userId} - Interni endpoint koji briše sve komentare koje je korisnik napisao. Koristi se kada se korisnik obriše, da bi se obrisali i svi njegovi komentari.
}