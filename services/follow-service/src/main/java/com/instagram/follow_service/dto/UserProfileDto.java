package com.instagram.follow_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String username;
    private String fname;
    private String lname;
    private String bio;
    private String profilePictureUrl;
    private Boolean privateProfile;
}