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

// Ovaj interfejs predstavlja repozitorijum za entitet PostMedia, koji se koristi za pristup tabeli "post_media" u bazi podataka.
// Sadrzi metode za pronalazenje svih medija fajlova povezanih sa odredjenom objavom (postId), brisanje svih medija fajlova povezani sa objavom, i brojanje medija fajlova povezani sa objavom.
// Napomene:
// - findByPostIdOrderByPositionAsc(Long postId) vraca listu medija fajlova povezanih sa datim postId, sortiranu po poziciji u rastucem redosledu.
// - deleteByPostId(Long postId) brise sve medija fajlove povezane sa datim postId.
// - countByPostId(Long postId) vraca broj medija fajlova povezanih sa datim postId.    