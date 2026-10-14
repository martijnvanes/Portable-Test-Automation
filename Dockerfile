# --- Build stage --------------------------------------------------------------
# Runs on the build machine's own architecture: the jar is platform independent,
# so there is no need to run Maven under (slow) emulation for the arm64 image.
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Dependencies first, so this layer is cached as long as pom.xml doesn't change
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# The tests call real public APIs; they run in CI before the image is built
RUN ./mvnw -B -q package -DskipTests

# --- Runtime stage ------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user; the reports directory is the only place it writes to
RUN groupadd --system tas && useradd --system --gid tas tas \
    && mkdir /app/reports && chown tas:tas /app/reports
USER tas

COPY --from=build /build/target/*.jar app.jar

ENV TAS_REPORTS_DIR=/app/reports \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
VOLUME /app/reports
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
