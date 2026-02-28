package com.instagram.post_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.instagram.post_service.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}

// Ovaj interfejs predstavlja repozitorijum za entitet Post, koji se koristi za pristup 
// tabeli "posts" u bazi podataka.
// Sadrzi metode za pronalazenje svih objava koje je korisnik kreirao (userId), sortirane 
// po vremenu kreiranja u opadajucem redosledu, i brojanje objava koje je korisnik kreirao.
// Napomene:    
// - findByUserIdOrderByCreatedAtDesc(Long userId) vraca listu objava koje je korisnik kreirao, 
// sortiranu po vremenu kreiranja u opadajucem redosledu.
// - countByUserId(Long userId) vraca broj objava koje je korisnik kreirao. Ova metoda moze
//  biti korisna za prikaz ukupnog broja objava na profilu koeisnika, kao i za analitiku i 
// statistiku unutra aplikacije.    