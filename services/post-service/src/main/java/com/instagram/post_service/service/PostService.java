package com.instagram.post_service.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private static final int MAX_MEDIA_PER_POST = 20;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final List<String> ALLOWED_VIDEO_TYPES = List.of(
        "video/mp4", "video/quicktime", "video/x-msvideo", "video/webm"
    );

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final RestTemplate restTemplate;

    public PostService(PostRepository postRepository, PostMediaRepository postMediaRepository, RestTemplate restTemplate) {
        this.postRepository = postRepository;
        this.postMediaRepository = postMediaRepository;
        this.restTemplate = restTemplate;
    }

    // ==================== КРЕИРАЊЕ ОБЈАВЕ ====================

    /**
     * Креирај објаву са медија фајловима.
     * Прво се креира пост, затим се upload-ују фајлови.
     */
    @Transactional
    public PostDto createPost(Long userId, String description, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Потребно је додати бар једну слику или видео.");
        }

        if (files.size() > MAX_MEDIA_PER_POST) {
            throw new IllegalArgumentException("Максималан број медија фајлова по објави је " + MAX_MEDIA_PER_POST + ".");
        }

        // Валидирај све фајлове пре чувања
        for (MultipartFile file : files) {
            validateFile(file);
        }

        // Креирај пост
        Post post = Post.builder()
            .userId(userId)
            .description(description)
            .createdAt(LocalDateTime.now())
            .build();
        post = postRepository.save(post);

        // Upload и сачувај медија фајлове
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

    // ==================== АЖУРИРАЊЕ ОПИСА ====================

    public PostDto updateDescription(Long postId, Long userId, String description) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Објава није пронађена."));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Немате дозволу за измену ове објаве.");
        }

        post.setDescription(description);
        post.setUpdatedAt(LocalDateTime.now());
        postRepository.save(post);

        return toDto(post);
    }

    // ==================== БРИСАЊЕ ОБЈАВЕ ====================

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("Објава није пронађена."));

        if (!post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Немате дозволу за брисање ове објаве.");
        }

        // Обриши медија фајлове са диска
        List<PostMedia> mediaList = postMediaRepository.findByPostIdOrderByPositionAsc(postId);
        for (PostMedia media : mediaList) {
            deleteFileFromDisk(media.getMediaUrl());
        }

        postMediaRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    // ==================== БРИСАЊЕ ПОЈЕДИНАЧНОГ МЕДИЈА ====================

    @Transactional
    public PostDto deleteMedia(Long postId, Long mediaId, Long userId) {
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

        // Провери да ли је ово последњи медија — ако јесте, обриши целу објаву
        long mediaCount = postMediaRepository.countByPostId(postId);
        if (mediaCount <= 1) {
            deleteFileFromDisk(media.getMediaUrl());
            postMediaRepository.delete(media);
            postRepository.delete(post);
            return null; // Објава обрисана
        }

        deleteFileFromDisk(media.getMediaUrl());
        postMediaRepository.delete(media);

        return toDto(post);
    }

    // ==================== ПРЕУЗИМАЊЕ ОБЈАВА ====================

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

    // ==================== ПОМОЋНЕ МЕТОДЕ ====================

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
            throw new IllegalArgumentException("Фајл " + file.getOriginalFilename() + " прелази максималну величину од 50 MB.");
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
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filepath = Paths.get(UPLOAD_DIR + filename);
        Files.createDirectories(filepath.getParent());
        Files.write(filepath, file.getBytes());
        return "/uploads/posts/" + filename;
    }

    private void deleteFileFromDisk(String mediaUrl) {
        try {
            String filename = mediaUrl.replace("/uploads/posts/", "");
            Path filepath = Paths.get(UPLOAD_DIR + filename);
            Files.deleteIfExists(filepath);
        } catch (IOException e) {
            log.warn("Неуспешно брисање фајла: {}", mediaUrl);
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

        // Преузми податке о кориснику
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