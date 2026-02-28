package com.instagram.post_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaDto {
    private Long id;
    private String mediaUrl;
    private String mediaType;
    private Integer position;
    private Long fileSize;
} 

// predstavljanje medija fajla koji je povezan sa objavom,
// ukljucujuci URL do fajla, tip medija, poziciju u objavi, i velicinu fajla. 