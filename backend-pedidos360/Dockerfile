# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
ENV MAVEN_OPTS="-Xmx768m"
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp package -DskipTests && \
    mkdir -p /healthcheck && \
    javac -d /healthcheck docker/DockerHealthcheck.java

FROM eclipse-temurin:17-jre-jammy AS runtime
ARG MODULE
WORKDIR /app
RUN mkdir -p /app/data /app/healthcheck && chown -R 10001:10001 /app
COPY --from=build --chown=10001:10001 /build/${MODULE}/target/*-1.0-SNAPSHOT.jar /app/app.jar
COPY --from=build --chown=10001:10001 /healthcheck/ /app/healthcheck/
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=192m -XX:ActiveProcessorCount=2"
HEALTHCHECK --interval=15s --timeout=8s --start-period=90s --retries=12 \
    CMD ["java", "-Xms8m", "-Xmx32m", "-cp", "/app/healthcheck", "DockerHealthcheck"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
