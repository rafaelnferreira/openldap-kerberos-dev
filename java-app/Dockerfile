FROM eclipse-temurin:21-jdk-jammy

# Install necessary packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /java-app

# Copy gradle wrapper and build files
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src src

# Build the application
RUN ./gradlew build -x test

# Create directory for keytab
RUN mkdir -p /etc

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "build/libs/java-app-0.0.1-SNAPSHOT.jar"]
