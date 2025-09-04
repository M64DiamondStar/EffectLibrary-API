# Use official Kotlin/Java base image
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy your compiled jar
COPY build/libs/effectlibrary-api-all.jar ./effectlibrary-api.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "effectlibrary-api.jar"]