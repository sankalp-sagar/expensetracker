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

# Debian-slim runtime (better JNI compatibility than Alpine for Tess4j when OCR is enabled)
FROM eclipse-temurin:21-jre
ARG MODULE
ENV MODULE_NAME=${MODULE}
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*
# Install Tesseract only for expense-service
RUN if [ "$MODULE" = "expense-service" ]; then \
        apt-get update && apt-get install -y --no-install-recommends \
            tesseract-ocr tesseract-ocr-eng libtesseract-dev libleptonica-dev \
        && rm -rf /var/lib/apt/lists/* ; \
    fi
COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
