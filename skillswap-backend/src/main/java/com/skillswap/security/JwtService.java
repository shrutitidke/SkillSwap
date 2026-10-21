package com.skillswap.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(String email) {
        return generateToken(email, 0);
    }

    public String generateToken(String email, long tokenVersion) {
        return Jwts.builder()
                .subject(email)
                .claim("tokenVersion", tokenVersion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSigningKey())
                .compact();
    }


    public String extractEmail(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }


    public boolean validateToken(String token, String email) {

        String tokenEmail = extractEmail(token);

        return tokenEmail.equals(email);
    }

    public boolean validateToken(
            String token,
            String email,
            long currentTokenVersion) {
        return extractEmail(token).equals(email)
                && extractTokenVersion(token) == currentTokenVersion;
    }

    public long extractTokenVersion(String token) {
        Object tokenVersion = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("tokenVersion");

        return tokenVersion instanceof Number number
                ? number.longValue()
                : 0;
    }
}
