# Testwigr Testing Documentation

## Table of Contents
1. [Testing Strategy Overview](#testing-strategy-overview)
2. [Test Types and Categorization](#test-types-and-categorization)
3. [Testing Environment Setup](#testing-environment-setup)
4. [Unit Testing Approach](#unit-testing-approach)
5. [Integration Testing Approach](#integration-testing-approach)
6. [Controller Testing Approach](#controller-testing-approach)
7. [End-to-End Testing Approach](#end-to-end-testing-approach)
8. [Security Testing Approach](#security-testing-approach)
9. [Test Data Management](#test-data-management)
10. [Test Fixtures and Utilities](#test-fixtures-and-utilities)
11. [Running Tests](#running-tests)
12. [Test Reports and Metrics](#test-reports-and-metrics)
13. [Continuous Integration](#continuous-integration)
14. [Troubleshooting Common Test Issues](#troubleshooting-common-test-issues)
15. [Test Coverage Goals](#test-coverage-goals)
16. [Future Testing Improvements](#future-testing-improvements)

## Testing Strategy Overview

The Testwigr application follows a comprehensive test strategy that ensures code quality, reliability, and correct behavior across all layers of the application. Our testing approach follows the testing pyramid model, with more unit tests at the base, followed by integration tests, and fewer complex end-to-end tests at the top.

### Core Testing Principles

1. **Isolation**: Tests should be independent of each other
2. **Repeatability**: Tests should produce the same results when run multiple times
3. **Automation**: All tests should be automated and part of the CI/CD pipeline
4. **Coverage**: Tests should cover all critical paths and business logic
5. **Fast Feedback**: Unit tests should run quickly to provide immediate feedback
6. **Real-world Scenarios**: End-to-end tests should simulate real user behavior

### Testing Framework

The Testwigr application uses the following testing technologies:

- **Spock Framework**: For writing expressive tests in Groovy
- **Spring Test**: For testing Spring Boot components
- **Spring Security Test**: For testing security configurations
- **MockMvc**: For testing controllers without starting a full web server
- **TestContainers**: For managing Docker-based test dependencies
- **JUnit 5**: For test execution and lifecycle management

## Test Types and Categorization

The Testwigr test suite is organized into the following categories:

### Unit Tests

Unit tests focus on testing individual components in isolation, with all dependencies mocked or stubbed. These tests verify that components behave correctly with controlled inputs and outputs.

**Naming Convention**: `*Spec.groovy` (for Spock tests)

**Packages**:
- `com.example.testwigr.service`: Service unit tests
- `com.example.testwigr.repository`: Repository-specific unit tests

### Integration Tests

Integration tests verify that components work correctly when integrated with their dependencies. These tests focus on the interaction between multiple components.

**Naming Convention**: `*IntegrationSpec.groovy`, `*IntegrationTest.groovy`

**Packages**:
- `com.example.testwigr.integration`: General integration tests
- `com.example.testwigr.service`: Service integration tests
- `com.example.testwigr.repository`: Repository integration tests

### Controller Tests

Controller tests verify the API endpoints, request handling, and response formatting. These tests focus on the controller layer and its interaction with services.

**Naming Convention**: `*ControllerSpec.groovy`, `*ControllerTest.groovy`

**Packages**:
- `com.example.testwigr.controller`: Controller tests

### Security Tests

Security tests verify authentication, authorization, and overall security configuration. These tests ensure that protected resources are accessible only to authorized users.

**Naming Convention**: `*SecuritySpec.groovy`, `*AuthenticationTest.groovy`

**Packages**:
- `com.example.testwigr.security`: Security tests

### End-to-End Tests

End-to-end tests simulate real user scenarios and verify that the complete system works as expected. These tests cover multiple components and their interactions.

**Naming Convention**: `*EndToEndTest.groovy`, `*FlowTest.groovy`

**Packages**:
- `com.example.testwigr.integration`: End-to-end test scenarios

## Testing Environment Setup

The Testwigr application uses a dedicated test environment with specific configurations for testing purposes.

### Test Profile

The application uses the Spring `test` profile for testing, which activates test-specific configurations:

- **Test Database**: MongoDB running on port 27018 (different from development)
- **Test JWT Secret**: A dedicated JWT secret for testing
- **Test Rate Limits**: Higher rate limits to prevent test failures
- **Detailed Logging**: More verbose logging for test diagnostics

### Test Properties

Test properties are defined in `src/test/resources/application-test.properties`:

```properties
# MongoDB connection for tests
spring.data.mongodb.uri=mongodb://localhost:27018/testdb
spring.data.mongodb.database=testdb

# Security settings for tests
app.jwt.secret=testSecretKeyForTestingPurposesOnlyDoNotUseInProduction
app.jwt.expiration=86400000

# Allow circular references during tests
spring.main.allow-bean-definition-overriding=true
spring.main.allow-circular-references=true
```

### Test Database

The test database is a dedicated MongoDB instance running on port 27018. This separation ensures that tests do not interfere with development data. The database is managed through Docker Compose:

```yaml
# docker/docker-compose-test.yml
services:
  mongodb-test:
    image: mongo:latest
    container_name: mongodb-test
    ports:
      - "27018:27017"
    environment:
      - MONGO_INITDB_DATABASE=testdb
```

### Test Database Utilities

The `TestDatabaseUtils` class provides methods for managing the test database:

- `cleanDatabase()`: Clears all data from the test database
- `populateDatabase()`: Populates the test database with sample data
- `createSocialNetwork()`: Creates a network of users with follow relationships
- `createComplexDatabaseScenario()`: Creates a complex test scenario with multiple entities

## Unit Testing Approach

Unit tests focus on testing the smallest components of the application in isolation. In Testwigr, unit tests primarily target service and utility classes.

### Service Unit Tests

Service unit tests verify the business logic in service classes. These tests use mocked dependencies to isolate the service under test.

**Example** (`UserServiceSpec.groovy`):

```groovy
class UserServiceSpec extends Specification {
    // Dependencies to be mocked
    UserRepository userRepository
    PasswordEncoder passwordEncoder
    UserService userService

    def setup() {
        userRepository = Mock(UserRepository)
        passwordEncoder = Mock(PasswordEncoder)
        userService = new UserService(userRepository, passwordEncoder)
    }

    def "should create a new user successfully"() {
        given: "a new user with complete data"
        def userToCreate = new User(
                username: 'testuser',
                email: 'test@example.com',
                password: 'password123',
                displayName: 'Test User'
        )

        and: "mocked repository and encoder behavior"
        userRepository.existsByUsername('testuser') >> false
        userRepository.existsByEmail('test@example.com') >> false
        passwordEncoder.encode('password123') >> 'encodedPassword'
        userRepository.save(_ as User) >> { User user -> user }

        when: "creating a new user"
        def result = userService.createUser(userToCreate)

        then: "user is created with correct attributes"
        result.username == 'testuser'
        result.email == 'test@example.com'
        result.password == 'encodedPassword'
        result.displayName == 'Test User'
    }
}

## Future Testing Improvements

As the Testwigr project evolves, we plan to implement several testing improvements:

### Short-Term Improvements

1. **Code Coverage Reporting**: Implement JaCoCo code coverage reporting to track test coverage
2. **Parameterized Tests**: Increase use of data-driven tests to cover more scenarios with less code
3. **Test Data Cleanup**: Enhance test cleanup to ensure complete isolation between tests
4. **Test Performance**: Optimize slow tests to reduce overall test execution time

### Medium-Term Improvements

1. **Contract Testing**: Implement contract tests to verify API compatibility
2. **Property-Based Testing**: Introduce property-based testing for edge cases
3. **Mutation Testing**: Implement mutation testing to verify test effectiveness
4. **Browser-Based Testing**: Add browser-based testing for future frontend components

### Long-Term Improvements

1. **Performance Testing**: Implement performance tests to verify system behavior under load
2. **Security Testing**: Add automated security testing with tools like OWASP ZAP
3. **Load Testing**: Implement load testing to verify system scalability
4. **Chaos Testing**: Introduce chaos testing to verify system resilience

### Implementation Plan

1. **Q1**: Implement code coverage reporting and test data cleanup
2. **Q2**: Introduce parameterized tests and optimize test performance
3. **Q3**: Implement contract testing and property-based testing
4. **Q4**: Add mutation testing and browser-based testing

These improvements will ensure that the Testwigr test suite remains robust and effective as the application grows and evolves.
```

### Repository Unit Tests

Repository unit tests verify that repository interfaces correctly interact with the database. These tests use an in-memory or test database.

**Example** (`PostRepositoryTest.groovy`):

```groovy
@DataMongoTest
@ActiveProfiles("test")
class PostRepositoryTest extends Specification {
    @Autowired
    PostRepository postRepository

    @Autowired
    UserRepository userRepository

    def setup() {
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "should find posts by user ID"() {
        given: "a user and some posts"
        def user = TestDataFactory.createUser()
        userRepository.save(user)

        def post1 = TestDataFactory.createPost(null, "Post 1", user.id, user.username)
        def post2 = TestDataFactory.createPost(null, "Post 2", user.id, user.username)
        postRepository.save(post1)
        postRepository.save(post2)

        when: "finding posts by user ID"
        def result = postRepository.findByUserId(user.id, PageRequest.of(0, 10))

        then: "correct posts are found"
        result.content.size() == 2
        result.content.any { it.content == "Post 1" }
        result.content.any { it.content == "Post 2" }
    }
}
```

## Integration Testing Approach

Integration tests verify that multiple components work correctly together. In Testwigr, integration tests focus on the interaction between services, repositories, and the database.

### Service Integration Tests

Service integration tests verify that services interact correctly with repositories and other services. These tests use a real test database.

**Example** (`PostServiceIntegrationTest.groovy`):

```groovy
@SpringBootTest
@ActiveProfiles('test')
class PostServiceIntegrationTest extends Specification {
    @Autowired
    PostService postService

    @Autowired
    UserService userService

    @Autowired
    PostRepository postRepository

    @Autowired
    UserRepository userRepository

    def setup() {
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "should create and retrieve a post"() {
        given: "a user in the database"
        def user = TestDataFactory.createUser()
        userRepository.save(user)

        when: "creating a post"
        def post = postService.createPost('Test content', user.id)

        then: "post is created with correct data"
        post.content == 'Test content'
        post.userId == user.id
        post.username == user.username

        when: "retrieving the post by ID"
        def retrievedPost = postService.getPostById(post.id)

        then: "the correct post is retrieved"
        retrievedPost.id == post.id
        retrievedPost.content == 'Test content'
    }
}
```

### Repository Integration Tests

Repository integration tests verify that repositories correctly interact with the database. These tests use a real test database.

**Example** (`UserRepositoryIntegrationSpec.groovy`):

```groovy
class UserRepositoryIntegrationSpec extends MongoIntegrationSpec {
    @Autowired
    UserRepository userRepository

    def cleanup() {
        userRepository.deleteAll()
    }

    def "should save and find users by username"() {
        given: "a user with a unique username"
        def user = TestDataFactory.createUser(null, 'uniqueusername')

        when: "saving and then retrieving the user by username"
        userRepository.save(user)
        def foundUser = userRepository.findByUsername('uniqueusername')

        then: "the user is found with correct attributes"
        foundUser.isPresent()
        foundUser.get().username == 'uniqueusername'
        foundUser.get().email == 'uniqueusername@example.com'
    }
}
```

## Controller Testing Approach

Controller tests verify the API endpoints, request handling, and response formatting. In Testwigr, controller tests use MockMvc to simulate HTTP requests.

### MockMvc Controller Tests

MockMvc controller tests verify the behavior of controllers without starting a full web server. These tests mock service dependencies.

**Example** (`UserControllerWebTest.groovy`):

```groovy
@WebMvcTest(controllers = [UserController.class])
@ActiveProfiles('test')
class UserControllerWebTest extends Specification {
    @Autowired
    MockMvc mockMvc

    @MockBean
    UserService userService

    @WithMockUser(username = 'testuser')
    def "should get user by username"() {
        given: "a username and mocked user service response"
        def username = 'testuser'
        def user = TestDataFactory.createUser('123', username)

        when(userService.getUserByUsername(anyString())).thenReturn(user)

        when: "requesting user profile by username"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/users/${username}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
        )

        then: "user profile is returned with correct information"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value(username))
    }
}
```

### Controller Integration Tests

Controller integration tests verify the complete request-response flow, including controller, service, and repository interactions. These tests use MockMvc with the full application context.

**Example** (`UserControllerIntegrationTest.groovy`):

```groovy
@SpringBootTest
class UserControllerIntegrationTest extends ControllerTestBase {
    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    def setup() {
        postRepository.deleteAll()
        userRepository.deleteAll()

        def user = TestDataFactory.createUser(null, 'testuser')
        user.password = passwordEncoder.encode('password')
        userRepository.save(user)
    }

    @WithMockUser(username = 'testuser')
    def "should get user profile"() {
        when: "requesting user profile"
        def result = mockMvc.perform(
                withAuth(MockMvcRequestBuilders.get('/api/users/testuser'))
        )

        then: "user profile is returned with correct information"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('testuser'))
                .andExpect(MockMvcResultMatchers.jsonPath('$.email').value('testuser@example.com'))
    }
}
```

## End-to-End Testing Approach

End-to-end tests simulate real user scenarios and verify that the complete system works as expected. In Testwigr, end-to-end tests cover user journeys from registration to logout.

### User Journey Tests

User journey tests verify complete user scenarios, such as registration, authentication, content creation, and social interactions.

**Example** (`ComprehensiveEndToEndTest.groovy`):

```groovy
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class ComprehensiveEndToEndTest extends Specification {
    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    ObjectMapper objectMapper

    def setup() {
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "should test the complete user journey"() {
        given: 'User registration data'
        def registerRequest = [
                username: 'journeyuser',
                email: 'journeyuser@example.com',
                password: 'journey123',
                displayName: 'Journey User'
        ]

        when: 'User registers'
        def registerResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
        ).andReturn()
        
        then: 'Registration is successful'
        registerResult.response.status == 200
        def registerJson = objectMapper.readValue(registerResult.response.contentAsString, Map)
        registerJson.success == true
        registerJson.username == 'journeyuser'

        when: 'User logs in'
        def loginRequest = [
                username: 'journeyuser',
                password: 'journey123'
        ]

        def loginResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn()

        then: 'Login is successful and token is returned'
        loginResult.response.status == 200
        def token = objectMapper.readValue(loginResult.response.contentAsString, Map).token

        // Additional steps...
    }
}
```

### Social Interaction Tests

Social interaction tests verify the social features of the application, such as following users, liking posts, and commenting.

**Example** (`SocialNetworkIntegrationTest.groovy`):

```groovy
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class SocialNetworkIntegrationTest extends Specification {
    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    def setup() {
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
        TestDatabaseUtils.createSocialNetwork(userRepository, passwordEncoder, 5)
    }

    def "should test basic social network interactions"() {
        given: 'A network of users'
        def users = userRepository.findAll()
        def posts = []

        when: 'Each user creates a post'
        // Test post creation

        then: 'Posts are created successfully'
        // Verify posts

        when: 'Users follow each other'
        // Test follow operations

        then: 'Follow operations succeed'
        // Verify follow relationships
    }
}
```

## Security Testing Approach

Security tests verify authentication, authorization, and overall security configuration. In Testwigr, security tests focus on JWT token validation, protected resource access, and user authentication.

### Authentication Tests

Authentication tests verify the user registration, login, and token generation processes.

**Example** (`AuthenticationFlowTest.groovy`):

```groovy
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class AuthenticationFlowTest extends Specification {
    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PasswordEncoder passwordEncoder

    def setup() {
        userRepository.deleteAll()
        def user = TestDataFactory.createUser(null, 'authuser')
        user.password = passwordEncoder.encode('password123')
        userRepository.save(user)
    }

    def "should register a new user"() {
        given: 'registration request data with all required fields'
        def requestBody = [
                username: 'newuser',
                email: 'newuser@example.com',
                password: 'newpassword',
                displayName: 'New User'
        ]

        when: 'sending registration request to the endpoint'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
        )

        then: 'registration is successful with OK status'
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
    }
}
```

### Token Validation Tests

Token validation tests verify that JWT tokens are properly validated and that expired or invalid tokens are rejected.

**Example** (`AuthenticationFlowIntegrationTest.groovy`):

```groovy
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class AuthenticationFlowIntegrationTest extends Specification {
    @Autowired
    MockMvc mockMvc

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    String jwtSecret

    def "should block access with expired token"() {
        given: 'An expired JWT token'
        def expiredToken = generateExpiredToken('activeuser', jwtSecret)

        when: 'Accessing protected resource with expired token'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/posts')
                        .header('Authorization', "Bearer ${expiredToken}")
        )

        then: 'Access is denied with 403 status'
        result.andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    private String generateExpiredToken(String username, String secret) {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS)

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(past.minus(2, ChronoUnit.DAYS)))
                .expiration(Date.from(past))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact()
    }
}
```

### Authorization Tests

Authorization tests verify that protected resources are accessible only to authorized users.

**Example** (`SecurityIntegrationSpec.groovy`):

```groovy
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class SecurityIntegrationSpec extends Specification {
    @Autowired
    MockMvc mockMvc

    def "should deny access to protected endpoints without authentication"() {
        when: "accessing a protected endpoint without authentication"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/posts')
        )

        then: "access is denied with forbidden status"
        result.andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    def "should allow access with valid JWT token"() {
        given: "a user logs in to get a token"
        // Login and get token

        when: "accessing a protected resource with the token"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/users/me')
                        .header('Authorization', "Bearer ${token}")
        )

        then: "access is granted to the protected resource"
        result.andExpect(MockMvcResultMatchers.status().isOk())
    }
}
```

## Test Data Management

Testwigr uses a comprehensive test data management approach to ensure consistent and reliable test data.

### Test Data Factory

The `TestDataFactory` class provides methods for creating test entities with consistent data. This ensures that test data is predictable and avoids duplication.

```groovy
class TestDataFactory {
    static User createUser(String id = null, String username = 'testuser') {
        def user = new User(
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
        if (id) {
            user.id = id
        }
        return user
    }

    static Post createPost(String id = null, String content = 'Test post', String userId = '123', String username = 'testuser') {
        def post = new Post(
                content: content,
                userId: userId,
                username: username,
                likes: [] as Set,
                comments: [],
                createdAt: LocalDateTime.now(),
                updatedAt: LocalDateTime.now()
        )
        if (id) {
            post.id = id
        }
        return post
    }

    // Additional factory methods...
}
```

### Test Database Utils

The `TestDatabaseUtils` class provides methods for managing the test database:

```groovy
class TestDatabaseUtils {
    static void cleanDatabase(UserRepository userRepository, PostRepository postRepository) {
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    static void createSocialNetwork(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    int userCount = 5) {
        // Create users and establish follow relationships
    }

    static User createTestUser(UserRepository userRepository, PasswordEncoder passwordEncoder, String username = "testuser") {
        // Create or retrieve a test user
    }

    // Additional utility methods...
}
```

### Test Data Scenarios

The testing framework supports different test data scenarios:

1. **Empty Database**: Tests start with a clean database to ensure isolation
2. **Basic Entities**: Tests create individual entities as needed
3. **Social Network**: Tests create a network of users with follow relationships
4. **Complex Scenario**: Tests create a comprehensive scenario with multiple entities and relationships

## Test Fixtures and Utilities

Testwigr provides several test fixtures and utilities to simplify test setup and execution.

### Base Test Classes

Base test classes provide common functionality for different test types:

1. **ControllerTestBase**: Base class for controller tests with authentication utilities
2. **MongoIntegrationSpec**: Base class for MongoDB integration tests
3. **SimpleSecurityTestConfig**: Simplified security configuration for tests

**Example** (`ControllerTestBase.groovy`):

```groovy
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SimpleSecurityTestConfig)
abstract class ControllerTestBase extends Specification {
    @Autowired
    protected MockMvc mockMvc

    @Autowired
    protected ObjectMapper objectMapper

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    protected String jwtSecret

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request, String username = "testuser") {
        String token = TestSecurityUtils.generateTestToken(username, jwtSecret)
        return request.header("Authorization", "Bearer ${token}")
    }

    // Additional utility methods...
}
```

### Test Security Utils

The `TestSecurityUtils` class provides utilities for security-related test setup:

```groovy
class TestSecurityUtils {
    static String generateTestToken(String username, String secret, long expirationDays = 10) {
        // Generate JWT token for testing
    }

    static String generateExpiredToken(String username, String secret) {
        // Generate expired JWT token for testing
    }

    static void setupAuthentication(UserDetails userDetails) {
        // Set up security context for testing
    }

    // Additional utility methods...
}
```

### Test Configurations

Test-specific configurations are provided to simplify test setup:

1. **TestMongoConfig**: MongoDB configuration for tests
2. **TestSecurityConfig**: Security configuration for tests
3. **SimpleSecurityTestConfig**: Simplified security configuration for tests

**Example** (`TestSecurityConfig.groovy`):

```groovy
@TestConfiguration
@EnableWebSecurity
class TestSecurityConfig {
    @Bean
    @Primary
    SecurityFilterChain testFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers('/api/auth/**').permitAll()
                    authorize.requestMatchers('/api/users/**').permitAll()
                    authorize.anyRequest().authenticated()
                })

        // Add JWT filter if needed
        
        return http.build()
    }

    // Additional beans...
}
```

## Running Tests

Testwigr provides several ways to run tests, from individual tests to the complete test suite.

### Running All Tests

To run all tests, use the Gradle test task:

```bash
./gradlew test
```

This command runs all tests in the project and generates test reports.

### Running Specific Test Categories

To run specific test categories, use the `--tests` parameter:

```bash
# Run all repository tests
./gradlew test --tests "com.example.testwigr.repository.*"

# Run all service tests
./gradlew test --tests "com.example.testwigr.service.*"

# Run all controller tests
./gradlew test --tests "com.example.testwigr.controller.*"

# Run all integration tests
./gradlew test --tests "com.example.testwigr.integration.*"
```

### Running Individual Tests

To run a specific test class, use the `--tests` parameter with the full class name:

```bash
./gradlew test --tests "com.example.testwigr.service.UserServiceSpec"
```

### Continuous Testing

For development, you can use continuous testing to automatically rerun tests when code changes:

```bash
./gradlew test --continuous
```

### Tests with Docker

To run tests with Docker, use the provided scripts:

```bash
# Start the test database
./scripts/start-test-db.sh

# Run tests
./gradlew test

# Stop the test database
./scripts/stop-test-db.sh
```

## Test Reports and Metrics

Testwigr generates comprehensive test reports to provide insight into test results and code coverage.

### JUnit Test Reports

JUnit XML test reports are generated automatically when running tests:

```
build/test-results/test/
```

These reports can be used by CI/CD systems to display test results.

### HTML Test Reports

HTML test reports provide a visual representation of test results:

```
build/reports/tests/test/index.html
```

These reports include:
- Test summary
- Test results by package and class
- Test execution time
- Test failures and errors

### Code Coverage Reports

Code coverage reports are not currently enabled, but can be added using JaCoCo:

```groovy
// In build.gradle
apply plugin: 'jacoco'

jacoco {
    toolVersion = "0.8.7"
}

jacocoTestReport {
    reports {
        xml.enabled true
        html.enabled true
    }
}

test {
    finalizedBy jacocoTestReport
}
```

This configuration generates code coverage reports in:

```
build/reports/jacoco/test/html/index.html
```

## Troubleshooting Common Test Issues

When working with the Testwigr test suite, you might encounter certain issues. Here are solutions to the most common problems:

### Path Variable Resolution Issues

**Symptom**: Tests fail with errors related to path variable resolution, particularly with `@PathVariable` annotations.

**Solution**: Ensure all `@PathVariable` annotations include explicit parameter names:

```groovy
// Incorrect
@PathVariable id String id

// Correct
@PathVariable("id") String id
```

### WebTestClient vs MockMvc Issues

**Symptom**: Tests using WebTestClient fail with configuration errors.

**Solution**: The project uses Spring MVC, not WebFlux, so tests should use MockMvc instead of WebTestClient:

```groovy
// Incorrect (WebTestClient - for WebFlux)
webTestClient
    .get()
    .uri("/api/users/me")
    .header("Authorization", "Bearer ${token}")
    .exchange()
    .expectStatus().isOk()

// Correct (MockMvc - for Spring MVC)
mockMvc.perform(
    MockMvcRequestBuilders.get("/api/users/me")
        .header("Authorization", "Bearer ${token}")
)
.andExpect(MockMvcResultMatchers.status().isOk())
```

### Authentication and JWT Issues

**Symptom**: Tests involving authentication fail with 401 or 403 errors.

**Solutions**:

1. **Check JWT Secret**: Ensure the JWT secret in tests matches the one in application-test.properties.

2. **JWT Token Generation**: Use TestSecurityUtils to generate valid tokens:

   ```groovy
   String token = TestSecurityUtils.generateTestToken(username, jwtSecret)
   ```

3. **Mock Authentication**: For controller tests, use Spring Security's test support:

   ```groovy
   @WithMockUser(username = "testuser")
   def "should get user profile"() {
       // Test code
   }
   ```

### Circular Dependency Issues

**Symptom**: The application context fails to initialize due to circular dependencies between services.

**Solution**: Use the `@Lazy` annotation to break circular dependencies:

```groovy
class ServiceA {
    private final @Lazy ServiceB serviceB

    ServiceA(@Lazy ServiceB serviceB) {
        this.serviceB = serviceB
    }
}
```

Ensure the application-test.properties includes:

```properties
spring.main.allow-circular-references=true
```

### MongoDB Connection Issues

**Symptom**: Tests fail with MongoDB connection errors.

**Solutions**:

1. **Check MongoDB Container**: Ensure the test MongoDB container is running:

   ```bash
   docker ps | grep mongodb-test
   ```

2. **Start Test Database**: Use the provided script:

   ```bash
   ./scripts/start-test-db.sh
   ```

3. **Check Port**: Ensure MongoDB is running on port 27018 (test port)

4. **Check Configuration**: Verify the MongoDB URI in application-test.properties:

   ```properties
   spring.data.mongodb.uri=mongodb://localhost:27018/testdb
   ```

### Test Data Management Issues

**Symptom**: Tests interfere with each other due to shared data.

**Solution**: Ensure tests clean up after themselves:

1. Use `setup()` and `cleanup()` methods in Spock tests:

   ```groovy
   def setup() {
       TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
   }

   def cleanup() {
       TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
   }
   ```

2. Use unique identifiers for test entities to avoid collisions

3. Run individual test classes when debugging data interference:

   ```bash
   ./gradlew test --tests "com.example.testwigr.integration.SpecificTestClass"
   ```

## Test Coverage Goals

The Testwigr project aims to maintain high test coverage to ensure code quality and reliability. While we don't currently enforce specific coverage percentages, we follow these guidelines:

### Coverage by Component Type

1. **Domain Models**: Simple verification of properties and methods
2. **Repositories**: 90%+ coverage of custom methods
3. **Services**: 85%+ coverage of business logic
4. **Controllers**: 80%+ coverage of endpoints and request handling
5. **Security Configuration**: 75%+ coverage of authentication and authorization logic
6. **Utility Classes**: 90%+ coverage of utility methods

### Coverage by Test Type

1. **Unit Tests**: Should cover core business logic and edge cases
2. **Integration Tests**: Should cover component interactions and data flow
3. **Controller Tests**: Should cover API contract and response handling
4. **End-to-End Tests**: Should cover critical user flows

### Coverage Verification

While we do not currently measure code coverage automatically, we plan to implement JaCoCo coverage reporting in the future. This will allow us to track coverage trends and identify areas for improvement.

## Continuous Integration

Testwigr includes CI/CD configuration to run tests automatically on code changes.

### GitHub Actions

The GitHub Actions workflow runs tests on each push:

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Start test MongoDB
        run: ./scripts/start-test-db.sh
      - name: Test with Gradle
        run: ./gradlew test
      - name: Stop test MongoDB
        run: ./scripts/stop-test-db.sh
```

### Test Status Summary

Here's a summary of our current test status:

#### Tests Passing

- **Controller tests**: Basic controller functionality and request handling
- **Integration tests**: Component interactions and data flow
- **Repository tests**: Data persistence and retrieval
- **Security tests**: Authentication and authorization flow
- **Service tests**: Business logic and service interactions

#### Test Coverage

Our test coverage is strong in core components, with some areas for future improvement:

- **High Coverage**: Repository methods, domain models, authentication flows
- **Medium Coverage**: Controller endpoints, service interactions
- **Areas for Improvement**: Edge cases, error handling

### CI Test Configuration

The Gradle build file includes CI-specific test configuration to ensure tests run reliably in the CI environment:

```groovy
tasks.named('test') {
    // Skip failing tests in CI until fixed
    if (System.getenv('CI') == 'true') {
        // Optionally ignore specific tests if needed
        // excludeTestsMatching "com.example.testwigr.controller.*"
    }
    
    // Make test output more verbose for debugging
    testLogging {
        events "passed", "skipped", "failed", "standardOut", "standardError"
        showExceptions true
        showCauses true
        showStackTraces true
        exceptionFormat "full"
    }
}
