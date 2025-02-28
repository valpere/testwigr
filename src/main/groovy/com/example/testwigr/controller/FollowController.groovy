package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/follow")
class FollowController {
    
    private final UserService userService
    
    FollowController(UserService userService) {
        this.userService = userService
    }
    
    @PostMapping("/{followingId}")
    ResponseEntity<Map<String, Object>> followUser(
            @PathVariable String followingId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())
        
        if (user.id == followingId) {
            return ResponseEntity.badRequest().body([error: "You cannot follow yourself"])
        }
        
        User updatedUser = userService.followUser(user.id, followingId)
        
        return ResponseEntity.ok([
            success: true,
            following: updatedUser.following.size(),
            isFollowing: true
        ])
    }
    
    @DeleteMapping("/{followingId}")
    ResponseEntity<Map<String, Object>> unfollowUser(
            @PathVariable String followingId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())
        
        User updatedUser = userService.unfollowUser(user.id, followingId)
        
        return ResponseEntity.ok([
            success: true,
            following: updatedUser.following.size(),
            isFollowing: false
        ])
    }
    
    @GetMapping("/followers")
    ResponseEntity<List<User>> getFollowers(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())
        
        List<User> followers = userService.getFollowers(user.id)
        
        return ResponseEntity.ok(followers)
    }
    
    @GetMapping("/following")
    ResponseEntity<List<User>> getFollowing(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())
        
        List<User> following = userService.getFollowing(user.id)
        
        return ResponseEntity.ok(following)
    }
    
    @GetMapping("/{userId}/status")
    ResponseEntity<Map<String, Object>> getFollowStatus(
            @PathVariable String userId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())
        
        User targetUser = userService.getUserById(userId)
        
        return ResponseEntity.ok([
            isFollowing: user.isFollowing(userId),
            isFollower: targetUser.isFollowing(user.id),
            followersCount: targetUser.followers.size(),
            followingCount: targetUser.following.size()
        ])
    }
}
