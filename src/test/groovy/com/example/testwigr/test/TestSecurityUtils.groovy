package com.example.testwigr.test

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
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
    
    // Mock authentication for a user
    static Authentication mockAuthentication(String username, String... roles) {
        def authorities = roles.collect { new SimpleGrantedAuthority("ROLE_${it}") }
        UserDetails userDetails = User.builder()
            .username(username)
            .password("password")
            .authorities(authorities)
            .build()
            
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities)
    }
    
    // Set authentication in SecurityContext - useful for tests
    static void setAuthentication(String username, String... roles) {
        SecurityContextHolder.getContext().setAuthentication(mockAuthentication(username, roles))
    }
    
    // Add authentication header to a request
    static MockHttpServletRequestBuilder addToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer ${token}")
    }

}
