# syntax=docker/dockerfile:1.7

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -e -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package && \
    mv target/*.jar /build/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && \
    mkdir -p /app/uploads && chown -R app:app /app
COPY --from=build --chown=app:app /build/app.jar /app/app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
