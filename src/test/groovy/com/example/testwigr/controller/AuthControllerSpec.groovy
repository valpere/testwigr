package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.service.UserService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import spock.lang.Specification

class AuthControllerSpec extends Specification {

    UserService userService
    AuthenticationManager authenticationManager
    AuthController authController

    def setup() {
        userService = Mock(UserService)
        authenticationManager = Mock(AuthenticationManager)
        authController = new AuthController(userService, authenticationManager)
    }

    def "should register a user successfully"() {
        given:
        def registerRequest = new AuthController.RegisterRequest(
            username: "testuser",
            email: "test@example.com",
            password: "password123",
            displayName: "Test User"
        )

        def createdUser = new User(
            id: "123",
            username: "testuser",
            email: "test@example.com",
            displayName: "Test User"
        )

        and:
        userService.createUser(_) >> createdUser

        when:
        def response = authController.registerUser(registerRequest)

        then:
        response.body.success
        response.body.userId == "123"
        response.body.username == "testuser"
    }

    def "should login a user successfully"() {
        given:
        def loginRequest = new AuthController.LoginRequest(
            username: "testuser",
            password: "password123"
        )

        def authentication = Mock(Authentication)

        and:
        authenticationManager.authenticate(_) >> authentication

        when:
        def response = authController.login(loginRequest)

        then:
        response.body.success
        response.body.message == "User logged in successfully"
    }

    def "should logout a user successfully"() {
        when:
        def response = authController.logout()

        then:
        response.body.success
        response.body.message == "User logged out successfully"
    }
}
