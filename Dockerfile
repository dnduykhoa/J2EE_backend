# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY pom.xml pom.xml
COPY src src

RUN chmod +x mvnw && ./mvnw -q -DskipTests package
RUN JAR_FILE=$(ls target/*.jar | grep -v '\.original$' | head -n 1) && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
