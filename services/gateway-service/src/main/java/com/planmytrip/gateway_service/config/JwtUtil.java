package com.planmytrip.gateway_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates JWTs issued by user-service. The gateway and user-service
 * MUST share the same signing secret (set JWT_SECRET as an env var in
 * both services) - the gateway does not issue tokens, only verifies them.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return the validated claims
     * @throws JwtException if the token is malformed, expired, or has a bad signature
     */
    public Claims validateAndExtractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token expired", e);
        } catch (Exception e) {
            throw new JwtException("Invalid token", e);
        }
    }

    public String extractUserId(Claims claims) {
        // Adjust "sub" / "userId" to whatever claim name user-service actually puts
        // the user id under when it issues the token.
        Object userId = claims.get("userId") != null ? claims.get("userId") : claims.getSubject();
        return userId != null ? userId.toString() : null;
    }
}
