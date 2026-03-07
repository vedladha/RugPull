package edu.wisc.t32.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for JWT.
 */
@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  private SecretKey getKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  /**
   * Generates a JWT token from the given email that expires after 1 day.
   *
   * @param email the email to generate the token for
   * @return the output token
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
   * Extracts an email from the given token.
   *
   * @param token the JWT token to get the email from
   * @return the email address
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
