# Stage 1: Build the application with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory
WORKDIR /app

# Copy the pom.xml file first
COPY pom.xml ./

# Download project dependencies using mvn directly
RUN mvn dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Package the application using mvn directly
RUN mvn package -DskipTests


# Stage 2: Create the final, smaller image
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Argument to specify the JAR file path
ARG JAR_FILE=target/online-course-management-0.0.1-SNAPSHOT.jar

# Copy the JAR file from the builder stage
COPY --from=builder /app/${JAR_FILE} app.jar

# Expose the port the application runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]