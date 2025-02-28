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
@RequestMapping("/api/feed")
class FeedController {
    
    private final FeedService feedService
    private final UserService userService
    
    FeedController(FeedService feedService, UserService userService) {
        this.feedService = feedService
        this.userService = userService
    }
    
    @GetMapping
    ResponseEntity<Page<Post>> getPersonalFeed(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())
        
        Page<Post> feed = feedService.getPersonalFeed(user.id, pageable)
        return ResponseEntity.ok(feed)
    }
    
    @GetMapping("/users/{username}")
    ResponseEntity<Page<Post>> getUserFeed(
            @PathVariable String username,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = userService.getUserByUsername(username)
        
        Page<Post> feed = feedService.getUserFeed(user.id, pageable)
        return ResponseEntity.ok(feed)
    }
}
