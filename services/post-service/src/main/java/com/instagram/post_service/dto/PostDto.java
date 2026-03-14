package com.instagram.post_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {
    private Long id;
    private Long userId;
    private String username;
    private String profilePictureUrl;
    private String description;
    private List<MediaDto> media;
    private Long likesCount;
    private Long commentsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}