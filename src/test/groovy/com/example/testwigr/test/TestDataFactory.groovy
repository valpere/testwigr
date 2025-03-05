package com.example.testwigr.test

import com.example.testwigr.model.Comment
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import java.time.LocalDateTime
import java.util.UUID
import java.util.stream.Collectors

class TestDataFactory {

    // Static map to store generated IDs to ensure consistency
    private static final Map<String, String> generatedIds = new HashMap<>()

    // Get a consistent ID for a given key
    static String getConsistentId(String key) {
        if (!generatedIds.containsKey(key)) {
            generatedIds.put(key, UUID.randomUUID().toString())
        }
        return generatedIds.get(key)
    }

    // User Factory Methods
    static User createUser(String id = null, String username = 'testuser') {
        // Ensure consistent ID generation if not explicitly provided
        String finalId = id ?: getConsistentId("user-" + username)

        def user = new User(
                id: finalId,
                username: username,
                email: "${username}@example.com",
                password: 'password123',
                displayName: username.capitalize(),
                createdAt: LocalDateTime.now(),
                updatedAt: LocalDateTime.now(),
                following: [] as Set,
                followers: [] as Set,
                active: true,
                bio: "Bio for ${username}"
        )
        return user
    }

    // Create a user with followers and following
    static User createUserWithConnections(String id = null, String username = 'networkuser',
                                          List<String> followerIds = [], List<String> followingIds = []) {
        def user = createUser(id, username)
        user.followers.addAll(followerIds)
        user.following.addAll(followingIds)
        return user
    }

    // Create an inactive user
    static User createInactiveUser(String id = null, String username = 'inactiveuser') {
        def user = createUser(id, username)
        user.active = false
        return user
    }

    // Create user with specific attributes
    static User createCustomUser(Map<String, Object> attributes) {
        def user = new User()
        attributes.each { key, value ->
            user[key] = value
        }
        // Set defaults for required fields if not provided
        if (!user.username) user.username = "custom${UUID.randomUUID().toString().substring(0, 8)}"
        if (!user.email) user.email = "${user.username}@example.com"
        if (!user.password) user.password = "password123"
        if (!user.createdAt) user.createdAt = LocalDateTime.now()
        if (!user.updatedAt) user.updatedAt = LocalDateTime.now()
        if (user.active == null) user.active = true
        if (user.following == null) user.following = [] as Set
        if (user.followers == null) user.followers = [] as Set
        // Set ID if not provided
        if (!user.id) user.id = getConsistentId("user-custom-" + user.username)

        return user
    }

    // Post Factory Methods
    static Post createPost(String id = null, String content = 'Test post', String userId = '123', String username = 'testuser') {
        // Ensure consistent ID generation if not explicitly provided
        String finalId = id ?: getConsistentId("post-" + content + "-" + userId)

        def post = new Post(
                id: finalId,
                content: content,
                userId: userId,
                username: username,
                likes: [] as Set,
                comments: [],
                createdAt: LocalDateTime.now(),
                updatedAt: LocalDateTime.now()
        )
        return post
    }

    // Create a post with likes
    static Post createPostWithLikes(String id = null, String content = 'Popular post',
                                    String userId = '123', String username = 'testuser',
                                    Set<String> likeUserIds = []) {
        def post = createPost(id, content, userId, username)
        post.likes.addAll(likeUserIds)
        return post
    }

    // Create a post with comments
    static Post createPostWithComments(String id = null, String content = 'Commented post',
                                       String userId = '123', String username = 'testuser',
                                       List<Comment> commentsToAdd = []) {
        def post = createPost(id, content, userId, username)
        post.comments.addAll(commentsToAdd)
        return post
    }

    // Create a post with specific date
    static Post createPostWithDate(String id = null, String content = 'Dated post',
                                   String userId = '123', String username = 'testuser',
                                   LocalDateTime createdAt) {
        def post = createPost(id, content, userId, username)
        post.createdAt = createdAt
        post.updatedAt = createdAt
        return post
    }

    // Create post with specific attributes
    static Post createCustomPost(Map<String, Object> attributes) {
        def post = new Post()
        attributes.each { key, value ->
            post[key] = value
        }
        // Set defaults for required fields if not provided
        if (!post.content) post.content = "Custom post content"
        if (!post.userId) post.userId = "defaultUserId"
        if (!post.username) post.username = "defaultUsername"
        if (!post.createdAt) post.createdAt = LocalDateTime.now()
        if (!post.updatedAt) post.updatedAt = LocalDateTime.now()
        if (post.likes == null) post.likes = [] as Set
        if (post.comments == null) post.comments = []
        // Set ID if not provided
        if (!post.id) post.id = getConsistentId("post-custom-" + post.content.substring(0, Math.min(10, post.content.length())))

        return post
    }

    // Comment Factory Methods
    static Comment createComment(String content = 'Test comment', String userId = '123', String username = 'testuser') {
        String commentId = getConsistentId("comment-" + content + "-" + userId)
        def comment = new Comment(content, userId, username)
        // Use reflection to set the ID since the Comment constructor doesn't accept an ID
        try {
            def field = Comment.class.getDeclaredField("id")
            field.setAccessible(true)
            field.set(comment, commentId)
        } catch (Exception e) {
            // Fallback to allow tests to work even if we can't set the ID
            System.err.println("Warning: Could not set consistent ID for comment: " + e.message)
        }
        return comment
    }

    // Create multiple comments
    static List<Comment> createComments(int count = 3, String baseContent = 'Comment',
                                        String userId = '123', String username = 'testuser') {
        def comments = []
        count.times { i ->
            comments << createComment("${baseContent} ${i+1}", userId, username)
        }
        return comments
    }

    // Create comment with specific date
    static Comment createCommentWithDate(String content = 'Dated comment',
                                         String userId = '123',
                                         String username = 'testuser',
                                         LocalDateTime createdAt) {
        def comment = createComment(content, userId, username)
        comment.createdAt = createdAt
        return comment
    }

    // Method to reset all stored IDs - useful for tests that need a clean state
    static void resetIds() {
        generatedIds.clear()
    }

    // Remaining methods... (keeping the existing social scenario methods)
    // Create a fully populated social scenario (users, posts, comments, likes)
    static Map<String, Object> createSocialScenario(int userCount = 3, int postsPerUser = 2,
                                                    int commentsPerPost = 2, int likesPerPost = 2) {
        def users = []
        def posts = []
        def result = [users: users, posts: posts]

        // Create users
        userCount.times { i ->
            def user = createUser(null, "user${i}")
            users << user
        }

        // Create posts, comments and likes
        users.each { user ->
            postsPerUser.times { i ->
                def post = createPost(null, "Post ${i} from ${user.username}", user.id, user.username)

                // Add comments from random users
                commentsPerPost.times { c ->
                    def commenter = users[c % users.size()]
                    post.comments << createComment("Comment on ${user.username}'s post", commenter.id, commenter.username)
                }

                // Add likes from random users
                likesPerPost.times { l ->
                    def liker = users[l % users.size()]
                    post.likes.add(liker.id)
                }

                posts << post
            }
        }

        return result
    }

    // Create a realistic timeline of posts with varied dates
    static List<Post> createTimeline(List<User> users, int postsPerUser = 5, int daysSpan = 30) {
        def posts = []
        def random = new Random()

        users.each { user ->
            postsPerUser.times { i ->
                // Create posts with dates distributed across the time span
                int daysAgo = random.nextInt(daysSpan)
                int hoursAgo = random.nextInt(24)
                LocalDateTime postDate = LocalDateTime.now()
                        .minusDays(daysAgo)
                        .minusHours(hoursAgo)

                def post = createPostWithDate(
                        null,
                        "Timeline post ${i} from ${user.username}",
                        user.id,
                        user.username,
                        postDate
                )

                // Maybe add some comments and likes
                if (random.nextBoolean()) {
                    int commentCount = random.nextInt(5) + 1
                    commentCount.times { c ->
                        def commenter = users[random.nextInt(users.size())]
                        int commentHoursAgo = random.nextInt(daysAgo * 24)
                        LocalDateTime commentDate = postDate.plusHours(commentHoursAgo % (24 * daysAgo))

                        post.comments << createCommentWithDate(
                                "Timeline comment on ${user.username}'s post",
                                commenter.id,
                                commenter.username,
                                commentDate
                        )
                    }
                }

                // Add some random likes
                int likeCount = random.nextInt(users.size() + 1)
                def shuffledUsers = new ArrayList<>(users)
                Collections.shuffle(shuffledUsers)
                shuffledUsers.subList(0, likeCount).each { liker ->
                    post.likes.add(liker.id)
                }

                posts << post
            }
        }

        // Sort posts by created date (newest first)
        return posts.sort { a, b -> b.createdAt <=> a.createdAt }
    }

    // Create a complex social network with realistic connections
    static Map<String, Object> createComplexSocialNetwork(int userCount = 10) {
        def users = []
        def posts = []
        def random = new Random()
        def result = [users: users, posts: posts]

        // Create users
        userCount.times { i ->
            def user = createUser(null, "netuser${i}")
            users << user
        }

        // Create follow relationships with realistic social network patterns
        // (Some users have many followers, some have few)
        users.each { user ->
            // Each user follows a random subset of other users
            def otherUsers = users.findAll { it.id != user.id }
            int followCount = random.nextInt(otherUsers.size())

            // Shuffle and take a random subset
            Collections.shuffle(otherUsers)
            def usersToFollow = otherUsers.subList(0, followCount)

            usersToFollow.each { userToFollow ->
                user.following.add(userToFollow.id)
                userToFollow.followers.add(user.id)
            }
        }

        // Create timeline of posts
        posts.addAll(createTimeline(users))

        return result
    }

}
