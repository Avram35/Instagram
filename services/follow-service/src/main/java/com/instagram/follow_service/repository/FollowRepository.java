package com.instagram.follow_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.instagram.follow_service.entity.Follow;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    List<Follow> findByFollowingId(Long followingId);
    List<Follow> findByFollowerId(Long followerId);
    long countByFollowingId(Long followingId);
    long countByFollowerId(Long followerId);
    void deleteByFollowerIdOrFollowingId(Long followerId, Long followingId);
}