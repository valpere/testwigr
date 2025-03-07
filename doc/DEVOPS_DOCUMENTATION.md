# Testwigr DevOps Documentation

## 1. Infrastructure Overview

The Testwigr platform employs a containerized microservices architecture designed for flexibility, scalability, and maintainability. Our infrastructure consists of several key components organized as follows:

### 1.1 Component Architecture

1. **Application Layer**: Spring Boot application running on JVM
   - RESTful API services
   - Authentication and authorization
   - Business logic processing
   - Data access layer

2. **Database Layer**: MongoDB for data persistence
   - Document-based storage
   - Flexible schema design
   - High-performance queries
   - Horizontal scaling capabilities

3. **Proxy Layer**: NGINX for frontend routing
   - SSL termination
   - Load balancing
   - Static content serving
   - Request filtering

4. **Monitoring Layer**: Prometheus and Grafana
   - Metrics collection
   - Performance visualization
   - Alerting system
   - Health monitoring

### 1.2 Containerization Benefits

Our containerized approach provides several significant advantages:

- **Environment Consistency**: Identical environments across development, testing, and production
- **Deployment Simplicity**: Streamlined deployment process with minimal manual intervention
- **Component Isolation**: Each service runs in its own container with defined resources
- **Portability**: Infrastructure can be deployed to any environment supporting Docker
- **Scalability**: Easy horizontal scaling of individual components
- **Versioning**: Clear versioning of infrastructure components

## 2. Containerization Strategy

### 2.1 Multi-Stage Builds

The application uses a multi-stage Dockerfile to optimize both build and runtime environments:

```dockerfile
# Builder stage
FROM openjdk:17-jdk-slim AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

# Runtime stage
FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN useradd -m appuser
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
```

This approach provides several benefits:
- Separates build dependencies from runtime dependencies
- Produces smaller final images
- Improves security by minimizing included components
- Optimizes caching during the build process

### 2.2 Container Security Measures

Our dockerization implements several security best practices:

- **Non-root User**: Containers run as a dedicated non-root user
- **Minimal Base Images**: Alpine-based images reduce attack surface
- **Resource Limits**: Explicit CPU and memory limits prevent resource exhaustion
- **Health Checks**: Regular verification of container health
- **Readonly Filesystems**: When possible, containers use read-only filesystems
- **Security-related JVM Flags**: Hardened JVM configuration

### 2.3 Environment-Specific Configurations

Separate Docker Compose files support different environments:

- `docker-compose.yml`: Development environment with hot-reloading
- `docker-compose-test.yml`: Testing environment with test databases
- `docker-compose-prod.yml`: Production environment with optimized settings

Each configuration includes appropriate resource limits, networking, and volume configurations for its intended use case.

## 3. CI/CD Pipeline

### 3.1 Pipeline Architecture

The Testwigr project includes CI/CD configuration using GitHub Actions, enabling a fully automated software delivery process:

```yaml
name: Testwigr CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Gradle
        run: ./gradlew build
      - name: Run tests
        run: ./gradlew test
      - name: Build Docker image
        run: docker build -t testwigr:latest .
      - name: Deploy to production
        if: github.ref == 'refs/heads/main'
        run: ./scripts/deploy-production.sh
```

### 3.2 Pipeline Stages

1. **Checkout**: Retrieves the latest source code from the repository
2. **Setup**: Configures the Java Development Kit and build tools
3. **Build**: Compiles the source code and packages the application
4. **Test**: Executes unit tests, integration tests, and code quality checks
5. **Package**: Creates the Docker image for deployment
6. **Deploy**: Automatically deploys to the target environment if all checks pass

### 3.3 Deployment Automation

The `deploy-production.sh` script automates the deployment process:

```bash
#!/bin/bash
set -e

# Load environment variables
source .env

# Verify required variables
if [ -z "$MONGODB_URI" ]; then
  echo "Error: MONGODB_URI is not set"
  exit 1
fi

# Create necessary directories
mkdir -p /var/log/testwigr
mkdir -p /var/data/testwigr/mongodb

# Stop existing containers
docker-compose -f docker/docker-compose-prod.yml down

# Start new containers
docker-compose -f docker/docker-compose-prod.yml up -d

# Verify deployment
echo "Waiting for application to start..."
sleep 10
curl -s http://localhost:8080/actuator/health | grep '{"status":"UP"}'
if [ $? -eq 0 ]; then
  echo "Deployment successful"
else
  echo "Deployment failed"
  exit 1
fi
```

This script ensures consistent deployments with proper error handling and verification.

## 4. Environment Management

### 4.1 Configuration Hierarchy

The Testwigr application utilizes a layered configuration approach:

1. **Base Configuration**: `application.properties` contains default settings
   ```properties
   server.port=8080
   spring.application.name=testwigr
   logging.level.root=INFO
   ```

2. **Environment Overrides**: Environment-specific properties files
   ```properties
   # application-dev.properties
   logging.level.root=DEBUG
   spring.data.mongodb.uri=mongodb://localhost:27017/testwigr
   ```

   ```properties
   # application-prod.properties
   logging.level.root=WARN
   server.tomcat.max-threads=200
   ```

3. **Runtime Overrides**: Environment variables take precedence
   ```
   SPRING_DATA_MONGODB_URI=mongodb://user:password@mongodb:27017/testwigr
   SERVER_PORT=8080
   ```

### 4.2 Sensitive Information Management

Sensitive configuration is managed through:

- **Environment Variables**: Used for production credentials
- **.env File**: Used for local development (excluded from version control)
- **CI/CD Secrets**: Stored securely in the CI/CD system
- **Spring Profiles**: Different profiles for dev/test/prod environments

Example `.env` template:
```
# Database Configuration
MONGODB_USERNAME=testwigr
MONGODB_PASSWORD=secretpassword
MONGODB_DATABASE=testwigr

# JWT Configuration
JWT_SECRET=your-256-bit-secret
JWT_EXPIRATION=86400000
```

## 5. Monitoring and Observability

### 5.1 Health Checks

The Testwigr application implements comprehensive health monitoring:

1. **Spring Boot Actuator**: Exposes health endpoints
   ```properties
   management.endpoints.web.exposure.include=health,info,metrics
   management.endpoint.health.show-details=when_authorized
   ```

2. **Docker Health Checks**: Container-level health verification
   ```yaml
   healthcheck:
     test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
     interval: 30s
     timeout: 10s
     retries: 3
     start_period: 40s
   ```

3. **Database Health**: MongoDB connection and query monitoring
   ```java
   @Component
   public class MongoHealthIndicator implements HealthIndicator {
       private final MongoTemplate mongoTemplate;
       
       @Override
       public Health health() {
           try {
               mongoTemplate.executeCommand("{ ping: 1 }");
               return Health.up().build();
           } catch (Exception e) {
               return Health.down()
                   .withDetail("error", e.getMessage())
                   .build();
           }
       }
   }
   ```

### 5.2 Metrics Collection

Prometheus is configured to collect comprehensive metrics:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-actuator'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['app:8080']
  
  - job_name: 'mongodb'
    static_configs:
      - targets: ['mongodb-exporter:9216']
  
  - job_name: 'nginx'
    static_configs:
      - targets: ['nginx-exporter:9113']
```

These metrics enable detailed monitoring of:
- Application performance (response times, error rates)
- JVM health (memory usage, garbage collection)
- Database performance (query times, connection pool)
- System resources (CPU, memory, disk, network)

### 5.3 Visualization

Grafana dashboards provide visualization of system health and performance:

1. **System Overview**: Overall health and key performance indicators
2. **Application Dashboard**: Request rates, response times, error rates
3. **JVM Dashboard**: Memory usage, garbage collection, threads
4. **MongoDB Dashboard**: Query performance, connection metrics
5. **Business Metrics**: User registrations, content creation, engagement

### 5.4 Log Management

The application uses a structured logging approach:

```xml
<appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
  <encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <includeMDC>true</includeMDC>
    <includeContext>false</includeContext>
    <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS</timestampPattern>
  </encoder>
</appender>
```

This format enables easy integration with log aggregation systems and facilitates log analysis.

## 6. Backup and Recovery

### 6.1 Automated Backup System

The MongoDB backup system uses a combination of scripts and scheduling:

```bash
#!/bin/bash
# backup-mongodb.sh

# Configuration
BACKUP_DIR="/var/backups/mongodb"
MONGODB_URI="${MONGODB_URI:-mongodb://localhost:27017/testwigr}"
RETENTION_DAYS=30

# Create timestamp
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_PATH="${BACKUP_DIR}/${TIMESTAMP}"

# Ensure backup directory exists
mkdir -p "$BACKUP_PATH"

# Perform backup
echo "Starting MongoDB backup to $BACKUP_PATH"
mongodump --uri="$MONGODB_URI" --out="$BACKUP_PATH"

# Compress backup
cd "$BACKUP_DIR"
tar -czf "${TIMESTAMP}.tar.gz" "${TIMESTAMP}"
rm -rf "${TIMESTAMP}"

# Remove old backups
find "$BACKUP_DIR" -name "*.tar.gz" -mtime +${RETENTION_DAYS} -delete

echo "Backup completed successfully"
```

### 6.2 Backup Schedule

Backups are scheduled via cron for regular execution:

```
# MongoDB backup schedule
0 2 * * * /opt/testwigr/scripts/backup-mongodb.sh > /var/log/testwigr/backup.log 2>&1
```

This configuration performs daily backups at 2:00 AM and logs the results.

### 6.3 Recovery Procedures

The documented recovery process includes:

1. **Assessment**: Determine the extent of data loss or corruption
2. **Selection**: Choose the appropriate backup to restore from
3. **Preparation**: Stop the application to prevent further changes
4. **Restoration**: Extract and restore the backup data
   ```bash
   # Extract backup
   tar -xzf 20250301_020000.tar.gz -C /tmp
   
   # Restore to MongoDB
   mongorestore --uri="$MONGODB_URI" --drop /tmp/20250301_020000
   ```
5. **Verification**: Confirm data integrity after restoration
6. **Restart**: Bring the application back online

## 7. Scaling Strategy

### 7.1 Vertical Scaling

Resource limits in Docker can be adjusted for vertical scaling:

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 1G
```

These settings can be increased as needed to accommodate higher workloads.

### 7.2 Horizontal Scaling

For larger deployments, the system can be horizontally scaled:

1. **Application Scaling**: Multiple application instances behind a load balancer
   ```yaml
   services:
     app:
       deploy:
         replicas: 3
   ```

2. **Database Scaling**: MongoDB replica sets for redundancy and read scaling
   ```yaml
   services:
     mongodb-primary:
       image: mongo:5
       command: --replSet rs0 --bind_ip_all
     
     mongodb-secondary-1:
       image: mongo:5
       command: --replSet rs0 --bind_ip_all
       
     mongodb-secondary-2:
       image: mongo:5
       command: --replSet rs0 --bind_ip_all
   ```

3. **Load Balancing**: NGINX configured to distribute traffic
   ```nginx
   upstream testwigr {
     server app-1:8080;
     server app-2:8080;
     server app-3:8080;
   }
   
   server {
     listen 80;
     location / {
       proxy_pass http://testwigr;
     }
   }
   ```

## 8. Security Implementation

### 8.1 Network Security

1. **NGINX SSL Configuration**: Proper TLS implementation
   ```nginx
   server {
     listen 443 ssl;
     ssl_certificate /etc/nginx/certs/testwigr.crt;
     ssl_certificate_key /etc/nginx/certs/testwigr.key;
     ssl_protocols TLSv1.2 TLSv1.3;
     ssl_ciphers HIGH:!aNULL:!MD5;
     ssl_prefer_server_ciphers on;
     ssl_session_cache shared:SSL:10m;
     ssl_session_timeout 10m;
   }
   ```

2. **Security Headers**: Protection against common web vulnerabilities
   ```nginx
   add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
   add_header X-Content-Type-Options "nosniff" always;
   add_header X-Frame-Options "SAMEORIGIN" always;
   add_header X-XSS-Protection "1; mode=block" always;
   add_header Content-Security-Policy "default-src 'self';" always;
   ```

3. **Docker Network Isolation**: Service segregation
   ```yaml
   networks:
     frontend:
       internal: false
     backend:
       internal: true
   ```

### 8.2 Application Security

1. **JWT Authentication**: Secure token-based authentication
   ```java
   @Configuration
   public class SecurityConfig extends WebSecurityConfigurerAdapter {
       @Override
       protected void configure(HttpSecurity http) throws Exception {
           http
               .cors().and().csrf().disable()
               .authorizeRequests()
               .antMatchers("/api/auth/**").permitAll()
               .anyRequest().authenticated()
               .and()
               .addFilter(new JwtAuthenticationFilter(authenticationManager()))
               .sessionManagement()
               .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
       }
   }
   ```

2. **Password Security**: Secure password storage
   ```java
   @Service
   public class UserService {
       @Autowired
       private PasswordEncoder passwordEncoder;
       
       public User createUser(UserDto userDto) {
           User user = new User();
           user.setUsername(userDto.getUsername());
           user.setEmail(userDto.getEmail());
           user.setPassword(passwordEncoder.encode(userDto.getPassword()));
           return userRepository.save(user);
       }
   }
   ```

3. **Rate Limiting**: Protection against abuse
   ```java
   @Component
   public class RateLimitingFilter extends OncePerRequestFilter {
       private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 requests per second
       
       @Override
       protected void doFilterInternal(HttpServletRequest request, 
                                      HttpServletResponse response, 
                                      FilterChain filterChain) throws ServletException, IOException {
           if (!rateLimiter.tryAcquire()) {
               response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
               response.getWriter().write("Rate limit exceeded");
               return;
           }
           
           filterChain.doFilter(request, response);
       }
   }
   ```

### 8.3 Infrastructure Security

1. **Non-root Containers**: Enhanced container security
   ```dockerfile
   RUN useradd -r -u 1001 -g appgroup appuser
   USER appuser
   ```

2. **Minimal Base Images**: Reduced attack surface
   ```dockerfile
   FROM openjdk:17-jre-alpine
   ```

3. **Regular Updates**: Automatic security patching
   ```yaml
   - name: Check for updates
     uses: renovatebot/renovate@v32
     with:
       schedule: "every weekend"
       packageRules:
         - matchUpdateTypes: ["minor", "patch", "pin", "digest"]
           automerge: true
   ```

## 9. Deployment Workflows

### 9.1 Development Workflow

For developers working on the application:

1. **Local Setup**:
   ```bash
   git clone https://github.com/yourusername/testwigr.git
   cd testwigr
   cp .env.template .env
   # Edit .env with appropriate values
   ```

2. **Start Development Environment**:
   ```bash
   docker-compose -f docker/docker-compose.yml up -d mongodb
   ./gradlew bootRun
   ```

3. **Access Development Tools**:
   - API Documentation: http://localhost:8080/swagger-ui.html
   - API Testing: http://localhost:8080/actuator/health

### 9.2 Testing Workflow

The testing workflow verifies changes before deployment:

1. **Run Unit Tests**:
   ```bash
   ./gradlew test
   ```

2. **Run Integration Tests**:
   ```bash
   docker-compose -f docker/docker-compose-test.yml up -d
   ./gradlew integrationTest
   docker-compose -f docker/docker-compose-test.yml down
   ```

3. **Review Test Reports**:
   - Unit Test Results: build/reports/tests/test/index.html
   - Integration Test Results: build/reports/tests/integrationTest/index.html
   - Code Coverage: build/reports/jacoco/test/html/index.html

### 9.3 Production Deployment Workflow

The production deployment process ensures reliability:

1. **Verify Build**:
   ```bash
   ./gradlew clean test build
   ```

2. **Deploy to Production**:
   ```bash
   ./scripts/deploy-production.sh
   ```

3. **Verify Deployment**:
   ```bash
   curl -s https://api.testwigr.com/actuator/health | grep '{"status":"UP"}'
   ```

4. **Monitor Deployment**:
   - Review logs for errors: `docker-compose -f docker/docker-compose-prod.yml logs -f`
   - Check Grafana dashboards for performance anomalies

## 10. Network Architecture

### 10.1 Docker Networks

The application uses multiple Docker networks for isolation:

```yaml
networks:
  app-network:
    driver: bridge
  frontend-network:
    driver: bridge
  monitoring-network:
    driver: bridge
```

These networks isolate different types of traffic:
- **app-network**: Internal communication between application and database
- **frontend-network**: External traffic from users to the application
- **monitoring-network**: Monitoring traffic for metrics and health checks

### 10.2 Network Flow

The typical request flow through the network follows this path:

1. User requests arrive at NGINX (port 80/443)
2. NGINX terminates SSL and forwards to the application (port 8080)
3. Application processes requests and communicates with MongoDB as needed
4. Responses flow back through the same path

This multi-tiered approach enhances security by limiting direct access to internal components.

### 10.3 External Access

In production, only specific ports are exposed externally:

```yaml
services:
  nginx:
    ports:
      - "80:80"
      - "443:443"
  
  grafana:
    ports:
      - "127.0.0.1:3000:3000"  # Restricted to localhost
```

All other services communicate only through internal Docker networks.

## 11. Resource Management

### 11.1 Container Resource Allocation

Each container is configured with appropriate resource limits:

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1.5G
        reservations:
          cpus: '0.5'
          memory: 1G
  
  mongodb:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 1G
```

These limits prevent resource starvation and ensure fair resource distribution.

### 11.2 JVM Configuration

The Java Virtual Machine is configured with container-aware settings:

```
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

These settings optimize memory usage and garbage collection for containerized environments.

### 11.3 Database Resources

MongoDB is configured with appropriate resource settings:

```yaml
services:
  mongodb:
    command: >
      --wiredTigerCacheSizeGB 1
      --setParameter maxConnections=1000
```

These parameters optimize MongoDB performance based on available resources.

## 12. Performance Tuning

### 12.1 JVM Tuning

The JVM is configured with performance-optimized settings:

```
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=4 -XX:ConcGCThreads=2 -XX:InitialHeapSize=1g -XX:MaxHeapSize=1g"
```

These settings balance throughput and responsiveness for the application.

### 12.2 Database Indexing

MongoDB collections are indexed to support common query patterns:

```javascript
// User collection indexes
db.users.createIndex({ "username": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });

// Posts collection indexes
db.posts.createIndex({ "userId": 1 });
db.posts.createIndex({ "createdAt": -1 });
db.posts.createIndex({ "tags": 1 });

// Comments collection indexes
db.comments.createIndex({ "postId": 1 });
db.comments.createIndex({ "userId": 1 });
```

These indexes dramatically improve query performance for common operations.

### 12.3 Content Caching

NGINX is configured to cache static content:

```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=STATIC:10m inactive=24h max_size=1g;

server {
    location ~* \.(css|js|jpg|jpeg|png|gif|ico|svg)$ {
        proxy_cache STATIC;
        proxy_cache_valid 200 302 1d;
        proxy_cache_use_stale error timeout updating http_500 http_502 http_503 http_504;
        proxy_cache_lock on;
        add_header X-Cache-Status $upstream_cache_status;
        expires 1d;
    }
}
```

This configuration reduces load on the application server for static resources.

## 13. High Availability Configuration

### 13.1 Application Redundancy

For high availability, multiple application instances are run concurrently:

```yaml
services:
  app:
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
        order: start-first
      restart_policy:
        condition: on-failure
        max_attempts: 3
        window: 120s
```

This configuration ensures that the application remains available even if individual instances fail.

### 13.2 Database Redundancy

MongoDB is configured as a replica set for data redundancy:

```yaml
services:
  mongodb-primary:
    image: mongo:5
    command: --replSet rs0 --bind_ip_all
    volumes:
      - mongodb_primary_data:/data/db
    networks:
      - app-network
    deploy:
      placement:
        constraints:
          - node.labels.role==db
  
  mongodb-secondary-1:
    image: mongo:5
    command: --replSet rs0 --bind_ip_all
    volumes:
      - mongodb_secondary1_data:/data/db
    networks:
      - app-network
    depends_on:
      - mongodb-primary
    deploy:
      placement:
        constraints:
          - node.labels.role==db
```

After deployment, the replica set is initialized with:

```javascript
rs.initiate({
  _id: "rs0",
  members: [
    { _id: 0, host: "mongodb-primary:27017", priority: 2 },
    { _id: 1, host: "mongodb-secondary-1:27017", priority: 1 }
  ]
})
```

### 13.3 Load Balancing

NGINX is configured to distribute traffic across application instances:

```nginx
upstream testwigr_backend {
    server app_1:8080;
    server app_2:8080;
    server app_3:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name testwigr.com www.testwigr.com;
    
    location / {
        proxy_pass http://testwigr_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

This configuration distributes incoming requests across all available instances.

## 14. Logging Strategy

### 14.1 Log Configuration

The application uses a structured logging approach:

```xml
<configuration>
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMDC>true</includeMDC>
            <includeContext>false</includeContext>
            <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS</timestampPattern>
            <fieldNames>
                <timestamp>timestamp</timestamp>
                <message>message</message>
                <logger>logger</logger>
                <thread>thread</thread>
                <level>level</level>
            </fieldNames>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE" />
    </root>
    
    <logger name="org.springframework" level="WARN" />
    <logger name="org.mongodb" level="WARN" />
    <logger name="com.testwigr" level="INFO" />
</configuration>
```

This configuration produces JSON-formatted logs suitable for aggregation and analysis.

### 14.2 Log Storage

In production, logs are collected and stored centrally:

```yaml
services:
  filebeat:
    image: docker.elastic.co/beats/filebeat:7.16.2
    volumes:
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
    user: root
    networks:
      - logging-network
```

The Filebeat configuration forwards logs to Elasticsearch:

```yaml
filebeat.inputs:
- type: container
  paths:
    - /var/lib/docker/containers/*/*.log
  processors:
    - add_docker_metadata:
        host: "unix:///var/run/docker.sock"

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "testwigr-%{+yyyy.MM.dd}"
```

### 14.3 Log Retention

Log retention policies are implemented to manage storage:

```yaml
services:
  elasticsearch:
    environment:
      - "ILM_POLICY_HOT_PHASE_ROLLOVER_MAX_AGE=1d"
      - "ILM_POLICY_WARM_PHASE_MOVE_AFTER=2d"
      - "ILM_POLICY_DELETE_PHASE_MOVE_AFTER=30d"
```

These settings ensure that logs are retained for 30 days before deletion.

## 15. Incident Response

### 15.1 Incident Classification

Incidents are classified by severity:

1. **Critical (P1)**: Service unavailable to all users
   - Response time: Immediate (< 15 minutes)
   - Resolution target: < 2 hours

2. **Major (P2)**: Significant functionality impaired for all users
   - Response time: < 1 hour
   - Resolution target: < 4 hours

3. **Minor (P3)**: Non-critical functionality impaired or affecting some users
   - Response time: < 4 hours
   - Resolution target: Next business day

4. **Cosmetic (P4)**: Visual or minor usability issues with minimal impact
   - Response time: Next business day
   - Resolution target: Within one week

### 15.2 Response Process

1. **Detection**: Identify the incident through monitoring alerts or user reports
2. **Classification**: Determine severity and priority based on impact
3. **Notification**: Alert the appropriate team members via automated channels
4. **Containment**: Implement immediate measures to limit impact
   - Service isolation
   - Traffic redirection
   - Feature disablement
5. **Investigation**: Determine root cause through log analysis and diagnostics
6. **Resolution**: Implement and deploy a fix for the underlying issue
7. **Recovery**: Restore normal service operation
8. **Post-Mortem**: Document the incident, response, and lessons learned

### 15.3 Incident Communication

1. **Internal Communication**: Team coordination via dedicated incident channel
2. **Status Page**: Public communication of service status
3. **Automated Alerts**: Generated based on monitoring thresholds
4. **Resolution Notification**: Updates to affected users when resolved

## 16. Capacity Planning

### 16.1 Resource Estimation

1. **User Capacity**:
   - 100 concurrent users: 1 application instance (1 CPU, 1GB RAM)
   - 500 concurrent users: 2-3 application instances (2 CPU, 1.5GB RAM each)
   - 1000+ concurrent users: 3+ application instances (2 CPU, 2GB RAM each)

2. **Storage Requirements**:
   - 100 active users: ~1GB/month
   - 1000 active users: ~10GB/month
   - 10000 active users: ~100GB/month

3. **Database Sizing**:
   - <1GB data: Single MongoDB instance
   - 1-10GB data: MongoDB with regular backups
   - >10GB data: MongoDB replica set with sharding

### 16.2 Load Testing

Before full production deployment, load testing should be performed:

1. **Define Test Scenarios**: Create realistic user journeys based on expected usage patterns
   ```
   - User registration and profile creation
   - Content creation and submission
   - Content browsing and searching
   - Comment and interaction workflows
   ```

2. **Configure Test Environment**: Set up an environment similar to production
   ```bash
   ./scripts/setup-loadtest-env.sh
   ```

3. **Execute Load Tests**: Run progressive load tests with JMeter
   ```bash
   ./scripts/run-load-tests.sh --users=50 --ramp-up=60 --duration=300
   ./scripts/run-load-tests.sh --users=100 --ramp-up=60 --duration=300
   ./scripts/run-load-tests.sh --users=200 --ramp-up=120 --duration=600
   ```

4. **Analyze Results**: Review performance metrics
   ```
   - Average response time
   - 95th percentile response time
   - Error rate
   - Throughput
   - Resource utilization (CPU, memory, network, disk)
   ```

5. **Identify Bottlenecks**: Determine system limitations
   ```
   - Database query performance
   - API endpoint response times
   - Resource constraints
   - Connection limits
   ```

6. **Optimize and Retest**: Implement improvements based on findings
   ```
   - Add or optimize indexes
   - Refactor slow endpoints
   - Adjust resource allocations
   - Implement caching
   ```

The load testing process helps identify performance issues before they impact real users and provides valuable data for capacity planning decisions.

### 16.3 Scaling Triggers

The system is configured to scale based on specific resource utilization triggers:

1. **CPU Utilization**: Scale when sustained CPU usage exceeds 70%
2. **Memory Utilization**: Scale when memory usage exceeds 80%
3. **Request Rate**: Scale when request rate exceeds 100 requests per second per instance
4. **Response Time**: Scale when 95th percentile response time exceeds 500ms

These triggers can be automated using container orchestration platforms like Kubernetes or Docker Swarm with appropriate monitoring and autoscaling configurations.

## 17. Compliance and Auditing

### 17.1 Audit Logging

The application implements comprehensive audit logging for security and compliance:

```java
@Aspect
@Component
public class AuditLogAspect {
    private static final Logger logger = LoggerFactory.getLogger("audit");
    
    @Autowired
    private SecurityService securityService;
    
    @Around("@annotation(auditable)")
    public Object logAuditEvent(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String userId = securityService.getCurrentUserId();
        String action = auditable.action();
        String resource = auditable.resource();
        
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("timestamp", new Date());
        auditData.put("userId", userId);
        auditData.put("action", action);
        auditData.put("resource", resource);
        auditData.put("method", joinPoint.getSignature().getName());
        auditData.put("params", Arrays.toString(joinPoint.getArgs()));
        
        try {
            Object result = joinPoint.proceed();
            auditData.put("status", "SUCCESS");
            logger.info(new ObjectMapper().writeValueAsString(auditData));
            return result;
        } catch (Exception e) {
            auditData.put("status", "FAILURE");
            auditData.put("error", e.getMessage());
            logger.info(new ObjectMapper().writeValueAsString(auditData));
            throw e;
        }
    }
}
```

This implementation captures detailed information about user actions:
- User identity
- Action performed
- Resource affected
- Timestamp
- Success/failure status
- Error details (if applicable)

### 17.2 Data Retention

The application implements configurable data retention policies:

```yaml
retention:
  # Audit logs retention
  audit-logs: 365d
  
  # Application logs retention
  application-logs: 90d
  
  # User data retention after account deletion
  user-data: 30d
  
  # Backup retention
  database-backups: 90d
```

These policies ensure that data is retained for the required period and then properly disposed of according to the retention schedule.

### 17.3 Access Controls

Role-based access control is implemented throughout the application:

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<UserDto> getAllUsers() {
    return userService.findAll();
}

@PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#userId)")
@GetMapping("/users/{userId}")
public UserDto getUser(@PathVariable String userId) {
    return userService.findById(userId);
}

@PreAuthorize("hasRole('MODERATOR')")
@PostMapping("/posts/{postId}/moderate")
public PostDto moderatePost(@PathVariable String postId, @RequestBody ModerationDto moderationDto) {
    return postService.moderate(postId, moderationDto);
}
```

This granular permission system ensures that users can only access resources they are authorized to see or modify.

## 18. Disaster Recovery Plan

### 18.1 Recovery Objectives

The disaster recovery plan is designed to meet specific recovery objectives:

1. **RPO (Recovery Point Objective)**: 24 hours
   - Daily database backups ensure that no more than 24 hours of data would be lost in a disaster

2. **RTO (Recovery Time Objective)**: 1 hour
   - The automated recovery process is designed to restore service within 1 hour of disaster declaration

These objectives represent the maximum acceptable data loss and downtime for the system.

### 18.2 Recovery Scenarios

The disaster recovery plan addresses several key scenarios:

1. **Infrastructure Failure**:
   ```
   # Infrastructure Recovery Procedure
   1. Activate standby infrastructure in alternate region
   2. Restore latest database backup
   3. Update DNS to point to standby environment
   4. Verify application functionality
   ```

2. **Data Corruption**:
   ```
   # Data Recovery Procedure
   1. Identify the point of corruption
   2. Stop the application to prevent further corruption
   3. Restore from the last known good backup
   4. Apply transaction logs up to the point of corruption if available
   5. Restart the application
   6. Verify data integrity
   ```

3. **Security Breach**:
   ```
   # Security Incident Recovery
   1. Isolate affected systems
   2. Deploy clean environment from verified images
   3. Restore data from pre-breach backup
   4. Apply security patches and updates
   5. Reset all credentials and secrets
   6. Verify security before returning to service
   ```

### 18.3 Recovery Testing

Regular recovery tests are conducted to ensure the effectiveness of the disaster recovery plan:

```
# Recovery Test Schedule
- Monthly: Database restore test
- Quarterly: Full application recovery test
- Semi-annually: Complete disaster recovery simulation
```

Each test includes:
- Documented test scenarios
- Success criteria
- Metrics collection (recovery time, data integrity)
- Post-test analysis and improvement plan

## 19. Cloud Deployment Options

### 19.1 AWS Deployment

The application can be deployed on Amazon Web Services using the following resources:

```terraform
provider "aws" {
  region = "us-west-2"
}

module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  version = "3.14.0"
  name = "testwigr-vpc"
  cidr = "10.0.0.0/16"
  azs = ["us-west-2a", "us-west-2b"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24"]
  public_subnets = ["10.0.101.0/24", "10.0.102.0/24"]
  enable_nat_gateway = true
}

module "ecs" {
  source = "terraform-aws-modules/ecs/aws"
  name = "testwigr-cluster"
  container_insights = true
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]
  default_capacity_provider_strategy = [
    {
      capacity_provider = "FARGATE"
      weight = 1
    }
  ]
}

module "mongodb" {
  source = "terraform-aws-modules/documentdb/aws"
  name = "testwigr-mongodb"
  master_username = var.mongodb_username
  master_password = var.mongodb_password
  instance_class = "db.r5.large"
  instances = {
    one = {}
    two = {}
  }
  vpc_id = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets
}
```

### 19.2 Azure Deployment

For Microsoft Azure, the application can be deployed using:

```terraform
provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "testwigr" {
  name     = "testwigr-resources"
  location = "East US"
}

resource "azurerm_container_registry" "acr" {
  name                = "testwigrregistry"
  resource_group_name = azurerm_resource_group.testwigr.name
  location            = azurerm_resource_group.testwigr.location
  sku                 = "Standard"
  admin_enabled       = true
}

resource "azurerm_kubernetes_cluster" "aks" {
  name                = "testwigr-aks"
  location            = azurerm_resource_group.testwigr.location
  resource_group_name = azurerm_resource_group.testwigr.name
  dns_prefix          = "testwigr-aks"

  default_node_pool {
    name       = "default"
    node_count = 2
    vm_size    = "Standard_D2_v2"
  }

  identity {
    type = "SystemAssigned"
  }
}

resource "azurerm_cosmosdb_account" "mongodb" {
  name                = "testwigr-mongodb"
  location            = azurerm_resource_group.testwigr.location
  resource_group_name = azurerm_resource_group.testwigr.name
  offer_type          = "Standard"
  kind                = "MongoDB"

  capabilities {
    name = "EnableMongo"
  }

  consistency_policy {
    consistency_level = "Session"
  }

  geo_location {
    location          = azurerm_resource_group.testwigr.location
    failover_priority = 0
  }
}
```

### 19.3 Google Cloud Platform Deployment

For Google Cloud, the application can be deployed using:

```terraform
provider "google" {
  project = var.project_id
  region  = "us-central1"
}

resource "google_container_cluster" "primary" {
  name     = "testwigr-gke"
  location = "us-central1"
  
  initial_node_count = 3
  
  node_config {
    preemptible  = false
    machine_type = "e2-standard-2"
    
    oauth_scopes = [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
      "https://www.googleapis.com/auth/devstorage.read_only"
    ]
  }
}

resource "google_compute_network" "vpc" {
  name                    = "testwigr-vpc"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "subnet" {
  name          = "testwigr-subnet"
  ip_cidr_range = "10.0.0.0/24"
  region        = "us-central1"
  network       = google_compute_network.vpc.id
}

resource "google_mongo_database_cluster" "mongodb" {
  name         = "testwigr-mongodb"
  project      = var.project_id
  region       = "us-central1"
  database_version = "MONGODB_4_2"
  
  cluster_type = "REGIONAL"
  
  master_instance {
    tier = "db-n1-standard-2"
    zone = "us-central1-a"
  }
}
```

## 20. Documentation Integration

### 20.1 Documentation Structure

The complete Testwigr documentation set consists of:

1. **Architecture Documentation**: System design, component interactions, and data flows
   - System overview
   - Component diagrams
   - Sequence diagrams
   - Data models
   - API specifications

2. **DevOps Documentation** (this document): Deployment, operations, and maintenance
   - Infrastructure setup
   - Deployment procedures
   - Monitoring configuration
   - Scaling strategies
   - Backup and recovery
   
3. **Testing Documentation**: Quality assurance and verification procedures
   - Test strategies
   - Test cases
   - Automation frameworks
   - Performance testing
   - Security testing

### 20.2 Documentation Maintenance

Documentation is maintained alongside code in the same repository:

```
/docs
  /architecture
    architecture-overview.md
    component-diagrams/
    sequence-diagrams/
    
  /devops
    deployment-guide.md
    monitoring-setup.md
    backup-procedures.md
    
  /testing
    test-strategy.md
    test-cases/
    performance-testing.md
```

Updates to documentation are required as part of the development workflow:

1. Code changes must include relevant documentation updates
2. Pull requests are reviewed for documentation completeness
3. Documentation is versioned alongside the codebase
4. Automated checks verify documentation quality and links

### 20.3 Documentation Access

The documentation is made available to team members through:

1. **GitHub Pages**: Hosted directly from the repository
2. **Internal Wiki**: For supplementary operational content
3. **API Documentation**: Generated from code using Swagger/OpenAPI

This approach ensures that documentation is always accessible and current.

## 21. Conclusion

This DevOps documentation provides a comprehensive guide to deploying, operating, and maintaining the Testwigr application. It covers all aspects of the application lifecycle, including:

- Infrastructure setup and configuration
- Containerization strategy and best practices
- CI/CD pipeline implementation
- Environment management and configuration
- Monitoring and observability solutions
- Backup and recovery procedures
- Scaling strategies for growth
- Security best practices and implementation
- Deployment workflows for different environments
- Network architecture and traffic flow
- Resource management and optimization
- Performance tuning guidelines
- High availability configuration
- Logging and analysis approaches
- Incident response protocols
- Capacity planning methodologies
- Compliance and auditing capabilities
- Disaster recovery planning
- Cloud deployment options
- Documentation integration and maintenance

By following these guidelines and procedures, the Testwigr application can be reliably deployed, operated, and maintained in any environment, from development to production. This documentation serves as a reference for DevOps engineers, developers, and system administrators responsible for the application's infrastructure and operations.

The documented practices balance modern DevOps principles with practical implementation details, ensuring that the Testwigr application can be efficiently managed throughout its lifecycle while maintaining high standards of reliability, security, and performance.
