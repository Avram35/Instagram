package com.instagram.post_service.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    /**
     * Ovde kreiram novu objavu koja moze sadrzati medija fajlove.
     * POST /api/v1/post
     * Content-Type: multipart/form-data
     *   - files: MultipartFile[] (обавезно, макс 20, макс 50MB по фајлу)
     *   - description: String (опционо)
     */
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

    /**
     * Ovaj deo koda omogucava korisniku da azurira opis objave koju je odabrao za azuriranje.
     * PUT /api/v1/post/{postId}
     */
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

    /**
     * Naredbe za brisanje cele objave.
     * DELETE /api/v1/post/{postId}
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long userId = postService.getUserIdByUsername(currentUser.getUsername());
        postService.deletePost(postId, userId);
        return ResponseEntity.ok(Map.of("message", "Објава је обрисана."));
    }

    /**
     * Naredbe za brisanje pojedinačnog medija iz objave. 
     * Ako se obrisu svi mediji ukljucujuci i poslednji koji je preostao, onda se brise i cela objava. 
     * Ne moze da postoji objava bez ijednog medija ili objava koja nema medije a ima opis. 
     * DELETE /api/v1/post/{postId}/media/{mediaId}
     */
    @DeleteMapping("/{postId}/media/{mediaId}")
    public ResponseEntity<?> deleteMedia(
        @PathVariable Long postId,
        @PathVariable Long mediaId,
        @AuthenticationPrincipal UserDetails currentUser
    ) {
        Long userId = postService.getUserIdByUsername(currentUser.getUsername());
        PostDto result = postService.deleteMedia(postId, mediaId, userId);
        if (result == null) {
            return ResponseEntity.ok(Map.of("message", "Објава је обрисана јер је уклоњен последњи медија."));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Preuzimanje objave po ID-u, sto treba da ukljuci i sve povezane medija fajlove. 
     * GET /api/v1/post/{postId}
     */
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPost(@PathVariable Long postId) {
        PostDto post = postService.getPostById(postId);
        return ResponseEntity.ok(post);
    }

    /**
     * Preuzmi sve objave koje je korisnik kreirao, ukljucujuci i sve povezane medija fajlove, za feed i profil. 
     * GET /api/v1/post/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDto>> getPostsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(postService.getPostsByUserId(userId));
    }

    /**
     * Naredba koja u sumira broj objava koje je korisnik kreirao 
     * Prikazuje se na samom profilu 
     * Moze biti korisno za prikaz ukupnog broja objava na profilu koeisnika, kao i za analitiku i statistiku unutra aplikacije. 
     * GET /api/v1/post/count/{userId}
     */
    @GetMapping("/count/{userId}")
    public ResponseEntity<Map<String, Long>> getPostCount(@PathVariable Long userId) {
        long count = postService.getPostCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}