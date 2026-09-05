FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY mvnw .
COPY .mvn ./.mvn
COPY pom.xml .
COPY src ./src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/time-deposit-kata-kotlin-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
