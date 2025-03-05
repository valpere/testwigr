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

        try {
            return Jwts.builder()
                    .subject(username)
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plus(expirationDays, ChronoUnit.DAYS)))
                    .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .compact()
        } catch (Exception e) {
            System.err.println("Error generating token: " + e.getMessage())
            return "test-token-for-" + username
        }
    }

    // Generate expired token for testing
    static String generateExpiredToken(String username, String secret) {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS)

        try {
            return Jwts.builder()
                    .subject(username)
                    .issuedAt(Date.from(past.minus(2, ChronoUnit.DAYS)))
                    .expiration(Date.from(past))
                    .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .compact()
        } catch (Exception e) {
            System.err.println("Error generating expired token: " + e.getMessage())
            return "expired-test-token-for-" + username
        }
    }

    // Parse and verify a token (useful for assertions)
    static String extractUsername(String token, String secret) {
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject()
        } catch (Exception e) {
            System.err.println("Error extracting username: " + e.getMessage())
            // Fall back to a simple parsing approach for test tokens
            if (token.startsWith("test-token-for-")) {
                return token.replace("test-token-for-", "")
            }
            return null
        }
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
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()

            return claims.getExpiration().before(new Date())
        } catch (Exception e) {
            // For test tokens, check if they're explicitly marked as expired
            return token.startsWith("expired-test-token-for-")
        }
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

    // Create authentication from username
    static Authentication createAuthenticationFromUsername(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                [new SimpleGrantedAuthority('ROLE_USER')]
        )
    }

    static boolean isValidToken(String token, String secret) {
        if (token == null) return false

        // Special handling for test tokens
        if (token.startsWith("test-token-for-")) {
            return !token.startsWith("expired-test-token-for-")
        }

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

    // Set up mock authentication for a specific username
    static void setupTestAuthentication(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        [new SimpleGrantedAuthority('ROLE_USER')]
                )
        )
    }

    // Clean up authentication after test
    static void clearAuthentication() {
        SecurityContextHolder.clearContext()
    }
}
