# Stage 1: build the application JAR with Gradle
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY config/ config/
COPY app/ app/

RUN chmod +x gradlew

RUN ./gradlew :app:build -x test

# Stage 2: create a minimal runtime image with only the built JAR
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/app-bundle.jar ./app-bundle.jar

CMD ["java", "-jar", "app-bundle.jar"]
