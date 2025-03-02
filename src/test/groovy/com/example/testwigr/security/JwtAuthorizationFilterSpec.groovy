package com.example.testwigr.security

import com.example.testwigr.model.User
import com.example.testwigr.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.userdetails.UserDetails
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class JwtAuthorizationFilterSpec extends Specification {

    AuthenticationManager authManager
    UserService userService
    String jwtSecret
    JwtAuthorizationFilter filter

    def setup() {
        authManager = Mock(AuthenticationManager)
        userService = Mock(UserService)
        jwtSecret = 'testSecretKeyForTestingPurposesOnlyDoNotUseInProduction'
        filter = new JwtAuthorizationFilter(authManager, userService, jwtSecret)
    }

    def "should authenticate user with valid token"() {
        given:
        def username = 'testuser'
        // def user = new User(id: '123', username: username)
        def userDetails = Mock(UserDetails)

        def token = Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 864000000)) // 10 days
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact()

        def request = Mock(HttpServletRequest)
        def response = Mock(HttpServletResponse)
        def filterChain = Mock(FilterChain)

        and:
        request.getHeader('Authorization') >> "Bearer ${token}"
        userService.loadUserByUsername(username) >> userDetails
        userDetails.getUsername() >> username
        userDetails.getAuthorities() >> []

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
    }

    def "should continue filter chain if no authorization header"() {
        given:
        def request = Mock(HttpServletRequest)
        def response = Mock(HttpServletResponse)
        def filterChain = Mock(FilterChain)

        and:
        request.getHeader('Authorization') >> null

        when:
        filter.doFilterInternal(request, response, filterChain)

        then:
        1 * filterChain.doFilter(request, response)
        0 * userService.loadUserByUsername(_)
    }

}
