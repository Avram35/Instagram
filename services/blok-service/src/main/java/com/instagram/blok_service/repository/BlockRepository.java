package com.instagram.blok_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.instagram.blok_service.entity.Block;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    List<Block> findByBlockerId(Long blockerId);
}