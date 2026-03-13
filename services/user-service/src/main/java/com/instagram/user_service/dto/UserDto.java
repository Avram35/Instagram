package com.instagram.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String fname;
    private String lname;
    private String bio;
    private String profilePictureUrl;
    private Boolean privateProfile;
}

