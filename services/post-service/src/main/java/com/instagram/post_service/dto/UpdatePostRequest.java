package com.instagram.post_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {
    private String description;
}

// DTO koji se koristi za azuriranje postojece objave, u ovom slucaju samo za azuriranje opisa objave 