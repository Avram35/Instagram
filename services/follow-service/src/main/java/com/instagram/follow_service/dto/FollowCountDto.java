package com.instagram.follow_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowCountDto {
    private Long userId;
    private Long followersCount;
    private Long followingCount;
}