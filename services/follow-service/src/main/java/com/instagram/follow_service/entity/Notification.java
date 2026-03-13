package com.instagram.follow_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recipientId;  
    private Long senderId;   
    private String type;       // FOLLOW, FOLLOW_REQUEST, LIKE, COMMENT
    private Long postId;       // ID објаве (за LIKE и COMMENT обавештења)
    private boolean read;

    private LocalDateTime createdAt;
}