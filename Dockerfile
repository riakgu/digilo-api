# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Copy maven wrapper and pom
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source
COPY src src

# Build without tests
RUN ./mvnw package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app
USER appuser

# Expose port
EXPOSE 8080

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
