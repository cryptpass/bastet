FROM gradle:8.13.0-jdk21 AS builder

WORKDIR /app
COPY . /app

RUN ./gradlew build --info -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/bastet-boot-* app.jar

CMD ["java", "-jar", "app.jar"]
