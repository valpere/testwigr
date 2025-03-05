package com.example.testwigr.controller

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.service.FeedService
import com.example.testwigr.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping('/api/follow')
class FollowController {

    private final UserService userService

    FollowController(UserService userService) {
        this.userService = userService
    }

    @PostMapping('/{followingId}')
    ResponseEntity<Map<String, Object>> followUser(
            @PathVariable("followingId") String followingId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        if (user.id == followingId) {
            return ResponseEntity.badRequest().body([error: 'You cannot follow yourself'] as Map<String, Object>)
        }

        User updatedUser = userService.followUser(user.id, followingId)

        return ResponseEntity.ok([
                success: true,
                following: updatedUser.following.size(),
                isFollowing: true
        ] as Map<String, Object>)
    }

    @DeleteMapping('/{followingId}')
    ResponseEntity<Map<String, Object>> unfollowUser(
            @PathVariable("followingId") String followingId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        User updatedUser = userService.unfollowUser(user.id, followingId)

        return ResponseEntity.ok([
                success: true,
                following: updatedUser.following.size(),
                isFollowing: false
        ] as Map<String, Object>)
    }

    @GetMapping('/followers')
    ResponseEntity<List<User>> getFollowers(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        List<User> followers = userService.getFollowers(user.id)

        return ResponseEntity.ok(followers)
    }

    @GetMapping('/following')
    ResponseEntity<List<User>> getFollowing(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        List<User> following = userService.getFollowing(user.id)

        return ResponseEntity.ok(following)
    }

    // New paginated endpoints
    @GetMapping('/followers/page')
    ResponseEntity<Page<User>> getFollowersPage(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Page<User> followers = userService.getFollowersPage(user.id, pageable)

        return ResponseEntity.ok(followers)
    }

    @GetMapping('/following/page')
    ResponseEntity<Page<User>> getFollowingPage(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Page<User> following = userService.getFollowingPage(user.id, pageable)

        return ResponseEntity.ok(following)
    }

    @GetMapping('/{userId}/status')
    ResponseEntity<Map<String, Object>> getFollowStatus(
            @PathVariable("userId") String userId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        User targetUser = userService.getUserById(userId)

        return ResponseEntity.ok([
                isFollowing: user.isFollowing(userId),
                isFollower: targetUser.isFollowing(user.id),
                followersCount: targetUser.followers.size(),
                followingCount: targetUser.following.size()
        ] as Map<String, Object>)
    }

}
