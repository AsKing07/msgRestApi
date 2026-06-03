package com.bschool.msgrestapi.security;

import com.bschool.msgrestapi.config.JwtProperties;
import com.bschool.msgrestapi.domain.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.expirationSeconds() * 1000L);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Number userId = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Number.class);
        return userId != null ? userId.longValue() : null;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            logger.warn("Token JWT expiré: {}", ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Token JWT invalide: {}", ex.getMessage());
        }
        return false;
    }

    private SecretKey key() {
        byte[] secretBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length >= 64) {
            return Keys.hmacShaKeyFor(secretBytes);
        }

        logger.warn("Clé JWT courte ({} octets) : dérivation SHA-512 appliquée.", secretBytes.length);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return Keys.hmacShaKeyFor(digest.digest(secretBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Impossible d'initialiser SHA-512 pour la clé JWT", e);
        }
    }
}
