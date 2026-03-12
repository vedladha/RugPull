FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY config/ config/
COPY app/ app/

RUN chmod +x gradlew

# build jar
RUN ./gradlew :app:build -x test

# run jar
CMD ["java", "-jar", "build/libs/app-bundle.jar"]
