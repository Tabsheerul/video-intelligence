# =====================================================
# Stage 1: Build the application with Maven
# =====================================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and POM first for better layer caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# =====================================================
# Stage 2: Run the application with a minimal JRE
# =====================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a non-root user for security
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create the uploads directory
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8081

# JVM tuning for containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
