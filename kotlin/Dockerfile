# Build stage: use the project Maven Wrapper for a reproducible build.
FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

# Copy Maven Wrapper and pom.xml first so dependency resolution
# can be cached separately from source code changes.
COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw -q dependency:go-offline

# Copy application sources only after dependencies are cached.
COPY src src

# Build the executable Spring Boot JAR.
RUN ./mvnw -q -DskipTests package


# Runtime stage: use a smaller JRE-only image.
FROM eclipse-temurin:17-jre

WORKDIR /app

# Run the application as a non-root user.
RUN groupadd --system app \
    && useradd --system --gid app app

# Copy only the built artifact from the build stage.
COPY --from=build \
    /workspace/target/app.jar \
    app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]