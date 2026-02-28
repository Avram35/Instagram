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
@Table(name = "post_media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ID je automatski generisan i sluzi kao primarni kljuc.

    @Column(name = "post_id", nullable = false)
    private Long postId;
    // postId je obavezan i predstavlja ID objave kojoj medija fajl pripada.

    @Column(name = "media_url", nullable = false)
    private String mediaUrl;
    // mediaUrl je obavezan i predstavlja URL do medija fajla.

    @Column(name = "media_type", nullable = false)
    private String mediaType; 
    //  mediaType je obavezan i predstavlja tip medija fajla, npr. "image" ili "video".

    @Column(name = "position", nullable = false)
    private Integer position; 
    // position je obavezan i predstavlja poziciju medija fajla u objavi, ako objava sadrzi vise fajlova (npr. karusel).

    @Column(name = "file_size")
    private Long fileSize; 
    // fileSize predstavlja velicinu medija fajla u bajtovima, moze biti null ako nije poznata.

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

// Ova klasa predstavlja entitet PostMedia koji se mapira na tabelu "post_media" u bazi podataka
// Sadrzi informacije o medija fajlu koji je povezan sa objavom,
// ukljucujuci URL do fajla, tip medija (slika ili video), poziciju u objavi (ako je vise fajlova), i velicinu fajla.
// Napomene:
// - ID je automatski generisan i sluzi kao primarni kljuc.
// - postId je obavezan i predstavlja ID objave kojoj medija fajl pripada.
// - mediaUrl je obavezan i predstavlja URL do medija fajla.
