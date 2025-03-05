package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification

class FeedServiceSpec extends Specification {

    PostRepository postRepository

    UserService userService
    FeedService feedService

    def setup() {
        postRepository = Mock(PostRepository)
        userService = Mock(UserService)
        feedService = new FeedService(postRepository, userService)
    }

    def "should get personal feed"() {
        given:
        def userId = '123'
        def followingId1 = '456'
        def followingId2 = '789'
        def user = new User(id: userId, username: 'user', following: [followingId1, followingId2] as Set)

        def posts = [
                new Post(id: 'p1', content: 'Post 1', userId: userId),
                new Post(id: 'p2', content: 'Post 2', userId: followingId1),
                new Post(id: 'p3', content: 'Post 3', userId: followingId2)
        ]

        def pageable = PageRequest.of(0, 10)
        def page = new PageImpl<>(posts, pageable, posts.size())

        and:
        userService.getUserById(userId) >> user
        postRepository.findByUserIdIn(_ as Collection, _ as Pageable) >> page
        // If the method exists, provide a mock implementation
        postRepository.respondsTo("findByUserIdInWithoutComments") >> false

        when:
        def result = feedService.getPersonalFeed(userId, pageable)

        then:
        result != null
        result.content.size() == 3
        result.content.find { it.id == 'p1' } != null
        result.content.find { it.id == 'p2' } != null
        result.content.find { it.id == 'p3' } != null
    }

    def "should get user feed"() {
        given:
        def userId = '123'
        def user = new User(id: userId, username: 'user')

        def posts = [
                new Post(id: 'p1', content: 'Post 1', userId: userId),
                new Post(id: 'p2', content: 'Post 2', userId: userId)
        ]

        def pageable = PageRequest.of(0, 10)
        def page = new PageImpl<>(posts, pageable, posts.size())

        and:
        userService.getUserById(userId) >> user
        postRepository.findByUserId(userId, pageable) >> page
        // If the method exists, provide a mock implementation
        postRepository.respondsTo("findByUserIdWithoutComments") >> false

        when:
        def result = feedService.getUserFeed(userId, pageable)

        then:
        result != null
        result.content.size() == 2
        result.content.every { it.userId == userId }
    }

    def "should get discovery feed"() {
        given:
        def posts = [
                new Post(id: 'p1', content: 'Popular post 1'),
                new Post(id: 'p2', content: 'Popular post 2'),
                new Post(id: 'p3', content: 'Popular post 3')
        ]

        def pageable = PageRequest.of(0, 10)
        def page = new PageImpl<>(posts, pageable, posts.size())

        and:
        postRepository.respondsTo("findPopularPosts") >> false
        postRepository.findAll(pageable) >> page

        when:
        def result = feedService.getDiscoveryFeed(pageable)

        then:
        result != null
        result.content.size() == 3
        result.content.find { it.id == 'p1' } != null
        result.content.find { it.id == 'p2' } != null
    }
}
