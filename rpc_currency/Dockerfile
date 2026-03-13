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

# Keep runtime outputs in /app (safe to bind-mount from host)
WORKDIR /app

# Store the executable jar outside /app so host bind mounts do not hide it
COPY --from=build /app/build/libs/app-bundle.jar /opt/rpc/app-bundle.jar

CMD ["java", "-jar", "/opt/rpc/app-bundle.jar"]
