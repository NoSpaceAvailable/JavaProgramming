# Repository Guidelines

## Project Structure & Module Organization

This Java 17 Maven multi-module project builds a Mini Discord-style chat app.

- `pom.xml` is the parent POM and defines shared dependency versions.
- `chat-common/` holds shared models, protocol classes, notifications, and JSON utilities.
- `chat-server/` holds the TCP server, handlers, services, repositories, database setup, and `server.properties`.
- `chat-client/` holds the JavaFX app, controllers, networking client, FXML views, CSS, and `client.properties`.
- Place tests in each module under `src/test/java`, mirroring the main package structure.

## Build, Test, and Development Commands

Run commands from the repository root unless noted.

- `mvn clean install` compiles and packages all modules.
- `mvn test` runs all module tests.
- `mvn -pl chat-common test` runs tests for a single module.
- `cd chat-server && mvn exec:java -Dexec.mainClass=com.lqc.server.ChatServer` starts the server.
- `cd chat-client && mvn javafx:run` starts the JavaFX client.

Before running the server, configure PostgreSQL in `chat-server/src/main/resources/server.properties`. The default database is `chat_app`.

## Coding Style & Naming Conventions

Use Java 17 features conservatively and follow the existing package layout. Indent Java, XML, FXML, and properties files with 4 spaces. Use `PascalCase` for classes and enums, `camelCase` for methods and fields, and `UPPER_SNAKE_CASE` for constants. Keep protocol classes grouped by purpose: `protocol/request`, `protocol/response`, and `protocol/notification`.

Prefer SLF4J logging over `System.out` except for short local diagnostics. Keep shared wire-format changes in `chat-common` and update client/server handling together.

## Testing Guidelines

JUnit 5 is available through the parent dependency management. Add tests near the module they validate, with names like `AuthServiceTest` or `JsonUtilTest`. Focus tests on protocol serialization, validation, authentication, repositories, and request dispatch behavior. For database-dependent tests, document required PostgreSQL setup and avoid local secrets.

Run `mvn test` before submitting changes. If a change affects startup or UI flow, also smoke test the server and client commands above.

## Commit & Pull Request Guidelines

Current history uses concise, imperative commit messages, for example `Rename project from 'LQC Chat' to 'Mini Discord'`. Keep commits focused on one logical change.

Pull requests should include a short summary, affected modules, test results, and configuration or database notes. Include screenshots or recordings for JavaFX UI changes, and link related issues when available.

## Security & Configuration Tips

Do not commit real database passwords or personal connection strings. Keep local values in `server.properties` and review diffs before committing. Uploaded files default to `./uploads`; avoid committing generated uploads, logs, or local database artifacts.
