# Multi-module Dockerfile. Build with: docker build --build-arg MODULE=auth-service -t splitwise/auth-service .
# Or use docker-compose which sets MODULE per service.
FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace
COPY pom.xml ./
COPY common-lib ./common-lib
COPY ${MODULE} ./${MODULE}
# Build only the requested module + its dependencies (common-lib)
RUN mvn -B -DskipTests -pl common-lib,${MODULE} -am install

FROM eclipse-temurin:21-jre-alpine
ARG MODULE
ENV MODULE_NAME=${MODULE}
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
