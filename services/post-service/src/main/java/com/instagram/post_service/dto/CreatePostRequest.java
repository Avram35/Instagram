package com.instagram.post_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {
    private String description;
}

//Ovaj DTO se koristi za kreiranje nove objave, ali u ovom slucaju ne sadrzi fajlove jer se 
// fajlovi salju kao multipart/form-data u PostControlleru 
// Ovaj DTO sadrzi samo opis objave, koji je opcionalan, i moze biti prazan string.