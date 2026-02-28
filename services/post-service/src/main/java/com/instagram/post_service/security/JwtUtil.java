package com.instagram.post_service.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }
}

// Ova klasa je  klasa za rad sa JWT tokenima. 
// Sadrzi metode za izvlačenje korisničkog imena iz tokena i validaciju tokena.
// U metodi init(), tajni kljuc se inicijalizuje nakon što se vrednost ${jwt.secret} 
// iz application.properties fajla učita u polje jwtSecret.
// Metoda getUsernameFromToken() koristi Jwts.parser() da parsira token i
//  izvadi korisničko ime (subject) iz tokena.
// Metoda validateJwtToken() pokušava da parsira token i vrati true 
// ako je token validan, ili false ako nije. Ako token nije validan,
//  hvata se JwtException ili IllegalArgumentException i loguje se greška.
