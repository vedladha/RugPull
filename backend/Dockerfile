#---Build Stage---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy dpeendency files
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY config/ config/
COPY libs/ libs/

# Cache dependencies for faster rebuild
RUN ./gradlew dependencies --no-daemon

# Build the application
COPY src/ src/
RUN ./gradlew build -x test --no-daemon



#---Run Stage---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Get the built application from the Build Stage
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
