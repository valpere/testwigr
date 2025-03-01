package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

import jakarta.validation.Valid

@RestController
@RequestMapping('/api/users')
@Tag(name = 'Users', description = 'User management API')
class UserController {

    private final UserService userService

    UserController(UserService userService) {
        this.userService = userService
    }

    @GetMapping('/{username}')
    @Operation(summary = 'Get user by username')
    ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username)
        return ResponseEntity.ok(user)
    }

    @PutMapping('/{id}')
    ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody User userDetails, Authentication authentication) {
        // Changed variable name from userDetails to authUserDetails to avoid conflict
        UserDetails authUserDetails = (UserDetails) authentication.getPrincipal()
        User authUser = userService.getUserByUsername(authUserDetails.getUsername())

        if (!authUser.id.equals(id)) {
            return ResponseEntity.status(403).build()
        }

        User updatedUser = userService.updateUser(id, userDetails)
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping('/{id}')
    ResponseEntity<?> deleteUser(@PathVariable String id, Authentication authentication) {
        // Changed variable name from userDetails to authUserDetails to avoid conflict
        UserDetails authUserDetails = (UserDetails) authentication.getPrincipal()
        User authUser = userService.getUserByUsername(authUserDetails.getUsername())

        if (!authUser.id.equals(id)) {
            return ResponseEntity.status(403).build()
        }

        userService.deleteUser(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping('/{id}/follow/{followingId}')
    ResponseEntity<User> followUser(@PathVariable String id, @PathVariable String followingId, Authentication authentication) {
        // Changed variable name from userDetails to authUserDetails to avoid conflict
        UserDetails authUserDetails = (UserDetails) authentication.getPrincipal()
        User authUser = userService.getUserByUsername(authUserDetails.getUsername())

        if (!authUser.id.equals(id)) {
            return ResponseEntity.status(403).build()
        }

        User updatedUser = userService.followUser(id, followingId)
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping('/{id}/unfollow/{followingId}')
    ResponseEntity<User> unfollowUser(@PathVariable String id, @PathVariable String followingId, Authentication authentication) {
        // Changed variable name from userDetails to authUserDetails to avoid conflict
        UserDetails authUserDetails = (UserDetails) authentication.getPrincipal()
        User authUser = userService.getUserByUsername(authUserDetails.getUsername())

        if (!authUser.id.equals(id)) {
            return ResponseEntity.status(403).build()
        }

        User updatedUser = userService.unfollowUser(id, followingId)
        return ResponseEntity.ok(updatedUser)
    }

    @GetMapping('/me')
    @Operation(summary = 'Get current user profile')
    ResponseEntity<User> getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())
        return ResponseEntity.ok(user)
    }

    @PutMapping('/me')
    @Operation(summary = 'Update current user profile')
    ResponseEntity<User> updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest updateUserRequest,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        User userToUpdate = new User()
        userToUpdate.displayName = updateUserRequest.displayName
        userToUpdate.bio = updateUserRequest.bio
        userToUpdate.email = updateUserRequest.email
        userToUpdate.password = updateUserRequest.password

        User updatedUser = userService.updateUser(user.id, userToUpdate)
        return ResponseEntity.ok(updatedUser)
            }

    @DeleteMapping('/me')
    @Operation(summary = 'Delete current user')
    ResponseEntity<?> deleteCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        userService.deleteUser(user.id)
        return ResponseEntity.ok([
            success: true,
            message: 'User deleted successfully'
        ])
    }

    static class UpdateUserRequest {

        String displayName
        String bio
        String email
        String password

    }

}
