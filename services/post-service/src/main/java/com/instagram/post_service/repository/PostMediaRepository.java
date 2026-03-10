package com.instagram.post_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.instagram.post_service.entity.PostMedia;

@Repository
public interface PostMediaRepository extends JpaRepository<PostMedia, Long> {
    List<PostMedia> findByPostIdOrderByPositionAsc(Long postId);
    void deleteByPostId(Long postId);
    long countByPostId(Long postId);
}