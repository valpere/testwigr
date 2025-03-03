package com.example.testwigr.test

import com.example.testwigr.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

class TestSecurityUtils {

    // Generate a test JWT token
    static String generateTestToken(String username, String secret, long expirationDays = 10) {
        Instant now = Instant.now()

        return Jwts.builder()
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationDays, ChronoUnit.DAYS)))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .compact()
    }

    // Generate expired token for testing
    static String generateExpiredToken(String username, String secret) {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS)

        return Jwts.builder()
            .subject(username)
            .issuedAt(Date.from(past.minus(2, ChronoUnit.DAYS)))
            .expiration(Date.from(past))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .compact()
    }

    // Parse and verify a token (useful for assertions)
    static String extractUsername(String token, String secret) {
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject()
    }

    // Add JWT token to a MockMvc request builder
    static MockHttpServletRequestBuilder addJwtToken(MockHttpServletRequestBuilder requestBuilder, String token) {
        return requestBuilder.header('Authorization', "Bearer ${token}")
    }

    // Create a mock authenticated request with JWT token
    static MockHttpServletRequest createAuthenticatedRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.addHeader('Authorization', "Bearer ${token}")
        return request
    }

    // Check if token is expired
    static boolean isTokenExpired(String token, String secret) {
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .getPayload()

        return claims.getExpiration().before(new Date())
    }

    // Set up authentication context for testing
    static void setupAuthentication(UserDetails userDetails) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        )
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    // Create authentication from user
    static Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
            user.username,
            null,
            [new SimpleGrantedAuthority('ROLE_USER')]
        )
    }

    static boolean isValidToken(String token, String secret) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()

            // Check if token is expired
            return !claims.getExpiration().before(new Date())
        } catch (Exception e) {
            return false
        }
    }
    // Clean up authentication after test
    static void clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

}
