package com.example.demo.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for generating and parsing JSON Web Tokens (JWT).
 *
 * <p>This component handles the creation of secure, time-limited tokens for user
 * authentication, as well as extracting and verifying claims from existing tokens.
 */
@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  private SecretKey getKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  /**
   * Generates a signed JWT for the specified user email.
   *
   * <p>The generated token includes the email as its subject and is configured
   * to expire 24 hours from the time of creation.
   *
   * @param email the user email to embed as the token subject
   * @return a compact, URL-safe JWT string representing the authenticated user
   */
  public String generateToken(String email) {
    return Jwts.builder()
        .subject(email)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
        .signWith(getKey())
        .compact();
  }

  /**
   * Extracts the email address from the subject claim of a valid JWT.
   *
   * <p>This method cryptographically verifies the token's signature using the
   * application's configured secret key before reading the payload.
   *
   * @param token the JWT string to parse and verify
   * @return the email address extracted from the token's subject claim
   * @throws io.jsonwebtoken.JwtException if the token is malformed, expired, or tampered with
   */
  public String extractEmail(String token) {
    return Jwts.parser()
        .verifyWith(getKey())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }
}
