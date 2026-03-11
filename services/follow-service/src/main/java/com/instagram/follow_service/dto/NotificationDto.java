package com.instagram.follow_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderProfilePicture;
    private String type;       // FOLLOW, FOLLOW_REQUEST, LIKE, COMMENT
    private Long postId;       // ID објаве (за LIKE и COMMENT обавештења)
    private boolean read;
    private LocalDateTime createdAt;
}