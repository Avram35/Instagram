package com.instagram.follow_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.instagram.follow_service.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
    void deleteByRecipientIdAndSenderId(Long recipientId, Long senderId);
    void deleteBySenderIdAndRecipientId(Long senderId, Long recipientId);
    void deleteByRecipientIdOrSenderId(Long recipientId, Long senderId);
    
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :userId AND n.read = false")
    void markAllAsReadByRecipientId(@Param("userId") Long userId);
}