package com.example.testwigr.config

import com.example.testwigr.repository.PostRepository
import com.example.testwigr.service.FeedService
import com.example.testwigr.service.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary

@Configuration
class ApplicationConfig {

    @Bean
    @Primary
    FeedService feedService(PostRepository postRepository,
                                   @Lazy UserService userService) {
        return new FeedService(postRepository, userService)
    }

}

