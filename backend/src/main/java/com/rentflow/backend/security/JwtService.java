package com.rentflow.backend.security;

import com.rentflow.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-ms:3600000}") long expirationMillis
    ) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret must not be blank. Configure app.jwt.secret via JWT_SECRET environment variable."
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "JWT secret must be a valid Base64 encoded string: " + e.getMessage(), e
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                "JWT secret key must be at least 256 bits (32 bytes) for HMAC-SHA256. Provided length: " + keyBytes.length + " bytes."
            );
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("role", user.getRole().name())
            .issuedAt(new Date(now))
            .expiration(new Date(now + expirationMillis))
            .signWith(key)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public long getExpirationMillis() {
        return expirationMillis;
    }
}
