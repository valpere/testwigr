package com.example.testwigr.security

import com.example.testwigr.model.User
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets
import java.util.Date

class JwtAuthenticationFilterMock extends UsernamePasswordAuthenticationFilter {

    private final String jwtSecret

    JwtAuthenticationFilterMock(String jwtSecret) {
        this.jwtSecret = jwtSecret
        setFilterProcessesUrl('/api/auth/login')
    }

    @Override
    Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginRequest loginRequest = new ObjectMapper().readValue(request.getInputStream(), LoginRequest)
            
            // For testing, automatically authenticate with ROLE_USER
            return new UsernamePasswordAuthenticationToken(
                loginRequest.username,
                null,
                [new SimpleGrantedAuthority("ROLE_USER")]
            )
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException {
        String username = authResult.getName()

        String token = Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 864000000)) // 10 days
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact()

        response.addHeader('Authorization', 'Bearer ' + token)
        response.contentType = 'application/json'
        response.characterEncoding = 'UTF-8'

        def responseBody = [
                username: username,
                token: token
        ]

        response.writer.write(new ObjectMapper().writeValueAsString(responseBody))
    }

    static class LoginRequest {
        String username
        String password
    }
}
