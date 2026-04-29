package com.restaurant.identity.service;

import com.restaurant.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Génération et vérification des JWT signés en RS256.
 *
 * - generateAccessToken : signe un nouveau token avec la clé privée
 * - parseClaims        : vérifie un token avec la clé publique et extrait son contenu
 *
 * Les autres microservices n'ont besoin QUE de la clé publique pour valider les tokens.
 * Ils n'ont pas besoin de cette classe — ils utiliseront spring-security-oauth2-resource-server.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final PrivateKey jwtPrivateKey;
    private final PublicKey jwtPublicKey;

    @Value("${jwt.expiration-minutes}")
    private long expirationMinutes;

    @Value("${jwt.issuer}")
    private String issuer;

    /**
     * Génère un access token JWT pour l'utilisateur donné.
     * Claims inclus : sub (userId), email, role, iss, iat, exp.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60);

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(jwtPrivateKey, Jwts.SIG.RS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }

    /**
     * Vérifie la signature et l'expiration du token, retourne ses claims.
     * Lève une exception si invalide.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtPublicKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }
}