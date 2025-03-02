package com.example.testwigr.security

import com.example.testwigr.service.UserService
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets

class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthorizationFilter.class)

    private final UserService userService
    private final String jwtSecret

    JwtAuthorizationFilter(AuthenticationManager authManager, UserService userService, String jwtSecret) {
        super(authManager)
        this.userService = userService
        this.jwtSecret = jwtSecret
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader('Authorization')

        if (header == null || !header.startsWith('Bearer ')) {
            chain.doFilter(request, response)
            return
        }

        try {
            UsernamePasswordAuthenticationToken authentication = getAuthentication(request)
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication)
            }
        } catch (Exception e) {
            logger.error('Failed to authenticate token: {}', e.getMessage())
            // Don't throw exception, let the request continue without authentication
        }

        chain.doFilter(request, response)
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader('Authorization')
        if (token != null) {
            try {
                String username = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token.replace('Bearer ', ''))
                    .getPayload()
                    .getSubject()

                if (username != null) {
                    UserDetails userDetails = userService.loadUserByUsername(username)
                    return new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    )
                }
            } catch (JwtException e) {
                logger.error('Invalid JWT token: {}', e.getMessage())
            } catch (Exception e) {
                logger.error('Authentication error: {}', e.getMessage())
            }
        }
        return null
    }

}
