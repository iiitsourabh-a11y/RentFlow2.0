package com.rentflow.backend.security;

import com.rentflow.backend.user.Role;
import com.rentflow.backend.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // A valid Base64-encoded 256-bit key for testing
    private static final String TEST_SECRET =
        Base64.getEncoder().encodeToString("test-secret-key-for-rentflow-dev-only-1234567890".getBytes());

    private static final String DIFFERENT_SECRET =
        Base64.getEncoder().encodeToString("different-secret-key-that-should-not-work-12345678".getBytes());

    private JwtService createService(String secret, long expirationMs) {
        return new JwtService(secret, expirationMs);
    }

    private User testUser() {
        try {
            Constructor<User> ctor = User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            User user = ctor.newInstance();
            user.setEmail("test@example.com");
            user.setRole(Role.TENANT);
            user.setFullName("Test");
            user.setPasswordHash("hashed");
            return user;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test User", e);
        }
    }

    @Test
    @DisplayName("generated token can be parsed back")
    void generateAndParse() {
        JwtService service = createService(TEST_SECRET, 3600000);
        User user = testUser();

        String token = service.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = service.parseToken(token);
        assertEquals("test@example.com", claims.getSubject());
        assertEquals("TENANT", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("token signed with different key is rejected")
    void wrongKeyRejected() {
        JwtService generator = createService(TEST_SECRET, 3600000);
        JwtService verifier = createService(DIFFERENT_SECRET, 3600000);

        String token = generator.generateToken(testUser());

        assertThrows(SignatureException.class, () -> verifier.parseToken(token));
    }

    @Test
    @DisplayName("expired token is rejected")
    void expiredTokenRejected() {
        // Create a service with 0ms expiration – token is immediately expired
        JwtService service = createService(TEST_SECRET, 0);
        String token = service.generateToken(testUser());

        assertThrows(ExpiredJwtException.class, () -> service.parseToken(token));
    }

    @Test
    @DisplayName("malformed token is rejected")
    void malformedTokenRejected() {
        JwtService service = createService(TEST_SECRET, 3600000);

        assertThrows(Exception.class, () -> service.parseToken("not.a.valid.jwt"));
    }

    @Test
    @DisplayName("empty token is rejected")
    void emptyTokenRejected() {
        JwtService service = createService(TEST_SECRET, 3600000);

        assertThrows(Exception.class, () -> service.parseToken(""));
    }

    @Test
    @DisplayName("getExpirationMillis returns configured value")
    void expirationMillis() {
        JwtService service = createService(TEST_SECRET, 7200000);
        assertEquals(7200000, service.getExpirationMillis());
    }

    @Test
    @DisplayName("null or blank secret throws IllegalStateException")
    void blankSecretThrows() {
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 3600000));
        assertThrows(IllegalStateException.class, () -> new JwtService("   ", 3600000));
    }

    @Test
    @DisplayName("secret with fewer than 256 bits throws IllegalArgumentException")
    void shortSecretThrows() {
        String shortBase64 = Base64.getEncoder().encodeToString("short-secret".getBytes());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> new JwtService(shortBase64, 3600000));
        assertTrue(ex.getMessage().contains("at least 256 bits"));
    }

    @Test
    @DisplayName("invalid base64 secret throws IllegalArgumentException")
    void invalidBase64SecretThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> new JwtService("not-valid-base-64!!!@@#$%", 3600000));
    }
}
