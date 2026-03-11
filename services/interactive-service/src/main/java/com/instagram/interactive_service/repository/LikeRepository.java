package com.instagram.interactive_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.instagram.interactive_service.entity.Like;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);
    List<Like> findByPostId(Long postId);
    long countByPostId(Long postId);
    void deleteByPostId(Long postId);
    void deleteByUserId(Long userId);
}