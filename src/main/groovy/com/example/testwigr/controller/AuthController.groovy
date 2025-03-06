package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

import jakarta.validation.Valid

/**
 * REST controller for handling authentication operations.
 * Provides endpoints for user registration, login, and logout.
 */
@RestController
@RequestMapping('/api/auth')
@Tag(name = "Authentication", description = "API endpoints for user registration, authentication, and session management")
class AuthController {

    private final UserService userService
    private final AuthenticationManager authenticationManager

    AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService
        this.authenticationManager = authenticationManager
    }

    /**
     * Registers a new user in the system.
     * Creates a new user account with the provided username, email, password, and display name.
     *
     * @param registerRequest Registration details including username, email, password, and optional display name
     * @return ResponseEntity containing the created user details
     */
    @PostMapping('/register')
    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with the provided username, email, and password. If display name is not provided, username will be used.",
        tags = ["Authentication"]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "User registered successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid input data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Username or email already exists",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Map<String, Object>> registerUser(
        @Valid @RequestBody RegisterRequest registerRequest
    ) {
        User user = new User(
            username: registerRequest.username,
            email: registerRequest.email,
            password: registerRequest.password,
            displayName: registerRequest.displayName ?: registerRequest.username
        )

        User createdUser = userService.createUser(user)

        return ResponseEntity.ok([
            success: true,
            userId: createdUser.id,
            username: createdUser.username
        ])
    }

    /**
     * Authenticates a user and provides a JWT token.
     * Validates user credentials and returns a JWT token for authenticated API access.
     *
     * @param loginRequest User credentials including username and password
     * @return ResponseEntity containing authentication token and status
     */
    @PostMapping('/login')
    @Operation(
        summary = "Authenticate a user and get token",
        description = "Validates user credentials and returns a JWT token that can be used for authenticated API access",
        tags = ["Authentication"]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Authentication successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid credentials",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Account disabled or locked",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Map<String, Object>> login(
        @Valid @RequestBody LoginRequest loginRequest
    ) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.username,
                loginRequest.password
            )
        )

        SecurityContextHolder.getContext().setAuthentication(authentication)

        // The JWT is generated by JwtAuthenticationFilter
        // This endpoint will return success with the token after successful authentication

        return ResponseEntity.ok([
            success: true,
            message: 'User logged in successfully'
        ])
    }

    /**
     * Logs out the current user.
     * Clears the security context and invalidates the user's session.
     * 
     * @return ResponseEntity containing logout status
     */
    @PostMapping('/logout')
    @Operation(
        summary = "Log out the current user",
        description = "Clears the security context and invalidates the user's session",
        tags = ["Authentication"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Logout successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Map<String, Object>> logout() {
        SecurityContextHolder.clearContext()

        return ResponseEntity.ok([
            success: true,
            message: 'User logged out successfully'
        ])
    }

    /**
     * Registration request DTO containing user details for account creation.
     */
    static class RegisterRequest {

        @NotBlank(message = 'Username cannot be empty')
        @Size(min = 3, max = 30, message = 'Username must be between 3 and 30 characters')
        @Schema(description = "User's unique username", example = "johndoe", required = true)
        String username

        @NotBlank(message = 'Email cannot be empty')
        @Email(message = 'Email must be valid')
        @Schema(description = "User's email address", example = "john.doe@example.com", required = true)
        String email

        @NotBlank(message = 'Password cannot be empty')
        @Size(min = 8, message = 'Password must be at least 8 characters')
        @Schema(description = "User's password", example = "password123", required = true)
        String password

        @Schema(description = "User's display name", example = "John Doe")
        String displayName

    }

    /**
     * Login request DTO containing user credentials for authentication.
     */
    static class LoginRequest {

        @NotBlank(message = 'Username cannot be empty')
        @Schema(description = "User's username", example = "johndoe", required = true)
        String username
        
        @NotBlank(message = 'Password cannot be empty')
        @Schema(description = "User's password", example = "password123", required = true)
        String password

    }

}
