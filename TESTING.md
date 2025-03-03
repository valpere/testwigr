# Testing Strategy for Testwigr

## Testing Layers

### Unit Tests

We use Spock Framework for expressive, behavior-driven tests of isolated components. Unit tests mock all dependencies to ensure isolation.

- **Service Layer Tests**: Test business logic in isolation from repositories
- **Controller Layer Tests**: Test REST endpoints with mocked services

### Slice Tests

Spring Boot slice tests focus on testing specific layers of the application.

- **Repository Tests**: Use `@DataMongoTest` to test repository interactions with MongoDB
- **Web Layer Tests**: Use `@WebMvcTest` to test controllers with mocked services

### Integration Tests

Tests that verify multiple components working together.

- **Service Integration Tests**: Test services with real repositories
- **Controller Integration Tests**: Test controllers with real services and repositories
- **Authentication Flow Tests**: Test the complete authentication process

### End-to-End Tests

Full API flow tests that simulate real user interactions from login to using features.

## Testing Tools

- **Spock Framework**: For expressive, behavior-driven tests
- **Spring Test**: For Spring context testing
- **MockMvc**: For testing REST endpoints
- **Docker**: For running a dedicated test MongoDB instance

## Test Configuration

- **MongoDB**: Tests run against a MongoDB instance in Docker
- **Test Profiles**: All tests use the `test` profile for appropriate configuration
- **Mock Users**: Authentication tests use pre-configured test users

## Running Tests

- Unit tests: `./gradlew test --tests "*.service.*"`
- Repository tests: `./gradlew test --tests "*.repository.*"`
- Controller tests: `./gradlew test --tests "*.controller.*"`
- Integration tests: `./gradlew test --tests "*.integration.*"`

### Tests passed

- ./gradlew test --tests "com.example.testwigr.integration.PostSocialIntegrationTest" --info
- ./gradlew test --tests "com.example.testwigr.integration.BasicApiIntegrationTest" --info
- ./gradlew test --tests "com.example.testwigr.integration.*" --info
- ./gradlew test --tests "com.example.testwigr.service.UserServiceSpec" --info
- ./gradlew test --tests "com.example.testwigr.service.PostServiceIntegrationTest" --info
- ./gradlew test --tests "com.example.testwigr.service.*" --info
- ./gradlew test --tests "com.example.testwigr.repository.*" --info
- ./gradlew test --tests "com.example.testwigr.controller.CommentControllerIntegrationSpec"
- ./gradlew test --tests "com.example.testwigr.controller.FollowControllerIntegrationSpec"

### Tests failed:

- ./gradlew test --tests "com.example.testwigr.controller.UserControllerIntegrationTest"
- ./gradlew test --tests "com.example.testwigr.controller.UserControllerWebTest"
- ./gradlew test --tests "com.example.testwigr.controller.*" --info
- ./gradlew test --tests "com.example.testwigr.security.AuthenticationFlowTest"
- ./gradlew test --tests "com.example.testwigr.security.SecurityIntegrationSpec"
- ./gradlew test --tests "com.example.testwigr.security.*" --info

## Test Data

We use a `TestDataFactory` class to provide consistent test data across all tests.

This ensures:

- Consistent entity creation
- Readable test code
- Easy maintenance when entity structures change

## Future Work

- Add full integration tests once unit and slice tests are stable
- Add performance tests for critical endpoints
- Add security testing for authentication flow

## Continuous Integration

All tests are automatically run as part of the CI/CD pipeline.
