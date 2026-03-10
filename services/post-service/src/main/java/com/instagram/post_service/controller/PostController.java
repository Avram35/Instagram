package com.instagram.post_service.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.instagram.post_service.dto.PostDto;
import com.instagram.post_service.dto.UpdatePostRequest;
import com.instagram.post_service.service.PostService;

@RestController
@RequestMapping("/api/v1/post")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createPost(
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam(value = "description", required = false) String description,
        @AuthenticationPrincipal UserDetails currentUser
    ) throws IOException {
        Long userId = postService.getUserIdByUsername(currentUser.getUsername());
        PostDto post = postService.createPost(userId, description, files);
        return ResponseEntity.ok(post);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<?> updateDescription(
        @PathVariable Long postId,
        @RequestBody UpdatePostRequest request,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long userId = postService.getUserIdByUsername(currentUser.getUsername());
        PostDto post = postService.updateDescription(postId, userId, request.getDescription());
        return ResponseEntity.ok(post);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails currentUser
    ) throws IOException {
        Long userId = postService.getUserIdByUsername(currentUser.getUsername());
        postService.deletePost(postId, userId);
        return ResponseEntity.ok(Map.of("message", "Објава је обрисана."));
    }

    @DeleteMapping("/{postId}/media/{mediaId}")
    public ResponseEntity<?> deleteMedia(
        @PathVariable Long postId,
        @PathVariable Long mediaId,
        @AuthenticationPrincipal UserDetails currentUser
    ) throws IOException {
        Long userId = postService.getUserIdByUsername(currentUser.getUsername());
        PostDto result = postService.deleteMedia(postId, mediaId, userId);
        if (result == null) {
            return ResponseEntity.ok(Map.of("message", "Објава је обрисана јер је уклоњена последња слика/видео."));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getPost(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long requesterId = postService.getUserIdByUsername(currentUser.getUsername());
        PostDto post = postService.getPostById(postId);

        if (!postService.canViewPosts(requesterId, post.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Немате приступ објавама овог корисника."));
        }

        // Ako je post null, to znaci da je obrisan (nema vise medija), ali korisnik je vlasnik posta

        return ResponseEntity.ok(post);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPostsByUser(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long requesterId = postService.getUserIdByUsername(currentUser.getUsername());

        if (!postService.canViewPosts(requesterId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Немате приступ објавама овог корисника."));
        }

        return ResponseEntity.ok(postService.getPostsByUserId(userId));
    }

    @GetMapping("/count/{userId}")
    public ResponseEntity<Map<String, Long>> getPostCount(@PathVariable Long userId) {
        long count = postService.getPostCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ==================== INTERNI ENDPOINTI ====================
    // Zasticeni InternalApiKeyFilter-om (X-Internal-Api-Key)

    @GetMapping("/internal/{postId}")
    public ResponseEntity<?> getPostInternal(@PathVariable Long postId) {
        try {
            PostDto post = postService.getPostById(postId);
            return ResponseEntity.ok(post);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Објава није пронађена."));
        }
    }
// Poziva ga feed-service da proveri da li je korisnik blokiran pre prikazivanja objava
    @GetMapping("/internal/user/{userId}")
    public ResponseEntity<List<PostDto>> getPostsByUserInternal(@PathVariable Long userId) {
        return ResponseEntity.ok(postService.getPostsByUserId(userId));
    }
    

    // Brisanje svih objava korisnika
    // Poziva ga auth-service pri deleteAccount()
    @DeleteMapping("/internal/user/{userId}")
    public ResponseEntity<Void> deleteAllPostsByUser(@PathVariable Long userId) {
        try {
            postService.deleteAllPostsByUser(userId);
        } catch (Exception e) {
        }
        return ResponseEntity.noContent().build();
    }
}
