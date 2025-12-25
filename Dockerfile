FROM amazoncorretto:21 as builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar

FROM amazoncorretto:21

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 20001

ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]