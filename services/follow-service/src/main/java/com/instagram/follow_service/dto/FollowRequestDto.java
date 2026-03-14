package com.instagram.follow_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowRequestDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String status;
    private LocalDateTime createdAt;
}