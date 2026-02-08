# Use official JDK 21 image
FROM eclipse-temurin:21-jdk-alpine

# Set workdir
WORKDIR /app

# Copy Maven wrapper + pom
COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Make Maven wrapper executable
RUN chmod +x mvnw

# Build the project
RUN ./mvnw clean package -DskipTests


# Expose port
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java","-jar","target/notification-engine-0.0.1-SNAPSHOT.jar"]
