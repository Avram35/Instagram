package com.instagram.post_service.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.instagram.post_service.dto.MediaDto;
import com.instagram.post_service.dto.PostDto;
import com.instagram.post_service.dto.UserProfileDto;
import com.instagram.post_service.entity.Post;
import com.instagram.post_service.entity.PostMedia;
import com.instagram.post_service.repository.PostMediaRepository;
import com.instagram.post_service.repository.PostRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PostService {

    private static final String UPLOAD_DIR = "/app/uploads/posts/";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final long MAX_TOTAL_REQUEST_SIZE = 20 * 50 * 1024 * 1024;
    private static final int MAX_MEDIA_PER_POST = 20;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png");
    private static final List<String> ALLOWED_VIDEO_TYPES = List.of("video/mp4", "video/mov");
    private static final int MAX_WIDTH  = 1080;
    private static final int MAX_HEIGHT = 1080;

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final RestTemplate restTemplate;

    // API kljuc za pozive ka interactive-service
    @Value("${internal.api.key}")
    private String internalApiKey;

    public PostService(PostRepository postRepository, PostMediaRepository postMediaRepository, RestTemplate restTemplate) {
        this.postRepository = postRepository;
        this.postMediaRepository = postMediaRepository;
        this.restTemplate = restTemplate;
    }

    // ==================== KREIRANJE OBJAVE ====================

    @Transactional
    public PostDto createPost(Long userId, String description, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Потребно је додати бар једну слику или видео.");
        }

        if (files.size() > MAX_MEDIA_PER_POST) {
            throw new IllegalArgumentException("Максималан број медија фајлова по објави је " + MAX_MEDIA_PER_POST + ".");
        }

        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalSize > MAX_TOTAL_REQUEST_SIZE) {
            throw new IllegalArgumentException("Укупна величина свих фајлова не сме прећи 1GB.");
        }

        if (description != null && description.length() > 2200) {
            throw new IllegalArgumentException("Опис не може бити дужи од 2200 карактера.");
        }

        for (MultipartFile file : files) {
            validateFile(file);
        }

        Post post = Post.builder()
            .userId(userId)
            .description(description != null ? description.trim() : null)
            .createdAt(LocalDateTime.now())
            .build();
        post = postRepository.save(post);

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String savedPath = saveFile(file);
            String mediaType = getMediaType(file.getContentType());

            PostMedia media = PostMedia.builder()
                .postId(post.getId())
                .mediaUrl(savedPath)
                .mediaType(mediaType)
                .position(i)
                .fileSize(file.getSize())
                .createdAt(LocalDateTime.now())
                .build();
            postMediaRepository.save(media);
        }

        return toDto(post);
    }

    // ==================== AZURIRANJE OPISA ====================

    public PostDto updateDescription(Long postId, Long userId, String description) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Објава није пронађена."));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Немате дозволу за измену ове објаве.");
        }

        if (description != null && description.length() > 2200) {
            throw new IllegalArgumentException("Опис не може бити дужи од 2200 карактера.");
        }

        post.setDescription(description != null ? description.trim() : null);
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);

        return toDto(post);
    }

    // ==================== BRISANJE OBJAVE ====================

    @Transactional
    public void deletePost(Long postId, Long userId) throws IOException {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Објава није пронађена."));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Немате дозволу за брисање ове објаве.");
        }

        deletePostInternal(post);
    }

    // Interni metod za brisanje objave
    @Transactional
    private void deletePostInternal(Post post) throws IOException {
        List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByPositionAsc(post.getId());
        for (PostMedia media : mediaList) {
            safeDeleteFileFromDisk(media.getMediaUrl());
        }

        deleteInteractionsForPost(post.getId());

        postMediaRepository.deleteByPostId(post.getId());
        postRepository.delete(post);
    }

    // Brisanje svih objava korisnika
    @Transactional
    public void deleteAllPostsByUser(Long userId) throws IOException {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (Post post : posts) {
            deletePostInternal(post);
        }
        log.info("Обрисано {} објава за корисника {}", posts.size(), userId);
    }

    // ==================== BRISANJE JEDNOG DELA POST-A ====================

    @Transactional
    public PostDto deleteMedia(Long postId, Long mediaId, Long userId) throws IOException {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Објава није пронађена."));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Немате дозволу за измену ове објаве.");
        }

        PostMedia media = postMediaRepository.findById(mediaId)
            .orElseThrow(() -> new RuntimeException("Медија фајл није пронађен."));

        if (!media.getPostId().equals(postId)) {
            throw new IllegalArgumentException("Медија не припада овој објави.");
        }

        long mediaCount = postMediaRepository.countByPostId(postId);
        if (mediaCount <= 1) {
            safeDeleteFileFromDisk(media.getMediaUrl());
            postMediaRepository.delete(media);
            deleteInteractionsForPost(postId);
            // Post ostaje bez medija, pa se briše
            postRepository.delete(post);
            return null;
        }

        safeDeleteFileFromDisk(media.getMediaUrl());
        postMediaRepository.delete(media);

        return toDto(post);
    }

    // ==================== PREUZIMANJE OBJAVA ====================

    public PostDto getPostById(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Објава није пронађена."));
        return toDto(post);
    }

    public List<PostDto> getPostsByUserId(Long userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public long getPostCount(Long userId) {
        return postRepository.countByUserId(userId);
    }

    // ==================== PROVERA PRISTUPA ====================

    public boolean canViewPosts(Long requesterId, Long postOwnerId) {
        if (requesterId.equals(postOwnerId)) return true;

        try {
            UserProfileDto owner = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/id/" + postOwnerId,
                UserProfileDto.class
            );

            if (owner == null || owner.getPrivateProfile() == null || !owner.getPrivateProfile()) {
                return true;
            }


            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map> followCheck = restTemplate.exchange(
                "http://follow-service:8083/api/v1/follow/check-internal/" + requesterId + "/" + postOwnerId,
                HttpMethod.GET,
                entity,
                Map.class
            );
            return followCheck.getBody() != null && Boolean.TRUE.equals(followCheck.getBody().get("following"));
        } catch (Exception e) {
            log.warn("Грешка при провери приступа: {}", e.getMessage());
            return false;
        }
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

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Фајл је празан.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Фајл прелази максималну величину од 50 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Непознат тип фајла.");
        }

        if (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_VIDEO_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Дозвољени су само фотографије и видео клипови.");
        }
    }

    private String getMediaType(String contentType) {
        if (contentType != null && contentType.startsWith("video/")) {
            return "video";
        }
        return "image";
    }

    private String saveFile(MultipartFile file) throws IOException {
        String ext = getExtension(file.getContentType());
        String filename = UUID.randomUUID() + ext;
        Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        Path filepath = uploadDir.resolve(filename).normalize();

        if (!filepath.startsWith(uploadDir)) {
            throw new IOException("Неисправна путања фајла.");
        }

        Files.createDirectories(filepath.getParent());

        String mediaType = getMediaType(file.getContentType());
        if ("image".equals(mediaType)) {
            byte[] resized = resizeImage(file.getBytes(), file.getContentType(), ext);
            Files.write(filepath, resized);
        } else {
            Files.write(filepath, file.getBytes());
        }

        return "/uploads/posts/" + filename;
    }

    private byte[] resizeImage(byte[] imageBytes, String contentType, String ext) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) {
            throw new IllegalArgumentException("Није могуће прочитати слику.");
        }

        if (original.getWidth() <= MAX_WIDTH && original.getHeight() <= MAX_HEIGHT) {
            return imageBytes;
        }

        BufferedImage resized = Scalr.resize(original, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH,MAX_WIDTH, MAX_HEIGHT);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String formatName = ext.replace(".", "");
        if ("jpg".equals(formatName)) formatName = "jpeg";
        ImageIO.write(resized, formatName, out);
        return out.toByteArray();
    }
    
    private String getExtension(String contentType) {
        if (contentType == null) return "";
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "video/mp4"  -> ".mp4";
            case "video/mov"  -> ".mov";
            default           -> "";
        };
    }

    private void safeDeleteFileFromDisk(String mediaUrl) {
        try {
            String filename = mediaUrl.replace("/uploads/posts/", "");
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Path filepath = uploadDir.resolve(filename).normalize();

            if (!filepath.startsWith(uploadDir)) {
                log.error("Path traversal покушај при брисању: {}", mediaUrl);
                return;
            }
            Files.deleteIfExists(filepath);
        } catch (IOException e) {
            log.warn("Грешка при брисању фајла {}: {}", mediaUrl, e.getMessage());
        }
    }

    private void deleteInteractionsForPost(Long postId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                "http://interactive-service:8087/api/v1/like/internal/post/" + postId,
                HttpMethod.DELETE, entity, Void.class
            );
        } catch (Exception e) {
            log.warn("Неуспешно брисање лајкова за објаву {}: {}", postId, e.getMessage());
        }
        try {
            restTemplate.exchange(
                "http://interactive-service:8087/api/v1/comment/internal/post/" + postId,
                HttpMethod.DELETE, entity, Void.class
            );
        } catch (Exception e) {
            log.warn("Неуспешно брисање коментара за објаву {}: {}", postId, e.getMessage());
        }
    }

    private PostDto toDto(Post post) {
        List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByPositionAsc(post.getId());
        List<MediaDto> mediaDtos = mediaList.stream()
            .map(m -> MediaDto.builder()
                .id(m.getId())
                .mediaUrl(m.getMediaUrl())
                .mediaType(m.getMediaType())
                .position(m.getPosition())
                .fileSize(m.getFileSize())
                .build()
            )
            .collect(Collectors.toList());

        String username = null;
        String profilePictureUrl = null;
        try {
            UserProfileDto profile = restTemplate.getForObject(
                "http://user-service:8082/api/v1/user/id/" + post.getUserId(),
                UserProfileDto.class
            );
            if (profile != null) {
                username = profile.getUsername();
                profilePictureUrl = profile.getProfilePictureUrl();
            }
        } catch (Exception e) {
            log.warn("Неуспешно преузимање корисника за пост {}: {}", post.getId(), e.getMessage());
        }

        long likesCount = 0;
        long commentsCount = 0;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> likesRes = restTemplate.getForObject(
                "http://interactive-service:8087/api/v1/like/count/" + post.getId(),
                Map.class
            );
            if (likesRes != null && likesRes.get("count") != null) {
                likesCount = Long.parseLong(likesRes.get("count").toString());
            }
        } catch (Exception e) {
            log.warn("Неуспешно преузимање лајкова: {}", e.getMessage());
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> commentsRes = restTemplate.getForObject(
                "http://interactive-service:8087/api/v1/comment/count/" + post.getId(),
                Map.class
            );
            if (commentsRes != null && commentsRes.get("count") != null) {
                commentsCount = Long.parseLong(commentsRes.get("count").toString());
            }
        } catch (Exception e) {
            log.warn("Неуспешно преузимање коментара: {}", e.getMessage());
        }

        return PostDto.builder()
            .id(post.getId())
            .userId(post.getUserId())
            .username(username)
            .profilePictureUrl(profilePictureUrl)
            .description(post.getDescription())
            .media(mediaDtos)
            .likesCount(likesCount)
            .commentsCount(commentsCount)
            .createdAt(post.getCreatedAt())
            .updatedAt(post.getUpdatedAt())
            .build();
    }
}