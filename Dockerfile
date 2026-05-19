# ---- Build stage: compile chat-common + chat-server into a runnable fat-jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the whole multi-module project (target/, .git, etc. excluded via .dockerignore)
COPY . .

# Build only the modules the server needs (-am = also build chat-common dependency).
# chat-client (JavaFX) is intentionally skipped — it runs natively outside Docker.
RUN mvn -B -ntp -pl chat-common,chat-server -am clean package -DskipTests

# ---- Runtime stage: small JRE image running the server jar ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# The shaded fat-jar produced by maven-shade-plugin
COPY --from=build /app/chat-server/target/chat-server-1.0-SNAPSHOT.jar app.jar

# Uploaded files live here (mounted as a volume in docker-compose)
RUN mkdir -p /app/uploads

EXPOSE 9000

# DB connection + port are injected via environment variables (see docker-compose.yml).
# UTC keeps message timestamps consistent regardless of host timezone.
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "app.jar"]
