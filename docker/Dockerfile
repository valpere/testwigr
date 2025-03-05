# Use a specific version for stability
FROM eclipse-temurin:21-jdk-alpine as builder

# Set working directory
WORKDIR /app

# Copy gradle files first to leverage Docker cache
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies (will be cached if no changes)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the application
RUN ./gradlew build -x test --no-daemon

# Create a slim production image
FROM eclipse-temurin:21-jre-alpine

# Create a dedicated user for the application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built artifact from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership to the non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Set health check
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Define entry point
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
