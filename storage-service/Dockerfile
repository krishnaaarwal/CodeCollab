# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
# Copy maven wrapper and pom
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make wrapper executable
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Run as non-root user for security
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser
USER appuser

# Expose Storage Service Port
EXPOSE 8084

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]