FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY config/ config/
COPY build.gradle settings.gradle ./

COPY src/ src/

RUN chmod +x gradlew

RUN ./gradlew build -x test

EXPOSE 8080

CMD ["java", "-jar", "build/libs/backend-0.0.1-SNAPSHOT.jar"]

