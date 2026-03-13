package com.instagram.follow_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.instagram.follow_service.entity.FollowRequest;
import com.instagram.follow_service.entity.FollowRequest.RequestStatus;

@Repository
public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    boolean existsBySenderIdAndReceiverIdAndStatus(Long senderId, Long receiverId, RequestStatus status);
    Optional<FollowRequest> findBySenderIdAndReceiverId(Long senderId, Long receiverId);
    List<FollowRequest> findByReceiverIdAndStatus(Long receiverId, RequestStatus status);
    boolean existsBySenderIdAndReceiverId(Long senderId, Long receiverId);
    void deleteBySenderIdOrReceiverId(Long senderId, Long receiverId);
}