package com.instagram.post_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String username;
    private String fname;
    private String lname;
    private String bio;
    private String profilePictureUrl;
    private Boolean privateProfile;
} 

// DTO koji predstavlja profil korisnika, ukljucujuci informacije kao sto su korisnicko ime,
//  ime korisnika
//  prezime korisnika, bio tj opis, URL do profilne slike, i informaciju o tome 
// da li je profil privatan ili javan