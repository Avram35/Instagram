package com.instagram.post_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
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
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ID objave, automatski generisan i sluzi kao primarni kljuc

    @Column(name = "user_id", nullable = false)
    private Long userId;
    // ID korisnika koji je kreirao objavu, obavezan

    @Column(columnDefinition = "TEXT")
    private String description;
    // Tekstualni opis objave, moze biti prazan

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    // Vreme kada je objava kreirana, obavezno

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    // Vreme kada je objava poslednji put azurirana, moze biti null ako objava nikada nije azurirana
}

// Ova klasa predstavlja entitet Post koji se mapira na tabelu "posts" u bazi podataka
// Sadrzi osnovne informacije o objavi, kao sto su ID, ID korisnika koji je kreirao objavu, opis objave, i vremenske oznake kreiranja i azuriranja objave.

