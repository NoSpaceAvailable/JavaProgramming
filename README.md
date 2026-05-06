# Mini Discord

A real-time multi-user chat application built with Java, inspired by Discord.

## Features (Planned)

- [x] User registration and login (BCrypt password hashing)
- [x] Group chat rooms (create, join, leave)
- [x] Private messaging (1-on-1 DMs)
- [x] File transfer (chunked upload/download)
- [x] Emoji reactions on messages
- [ ] Online/Away/Offline status indicators
- [ ] Message history with pagination
- [ ] Discord-inspired dark theme UI

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Build | Maven (multi-module) |
| Server | Java Socket + Multithreading |
| Client UI | JavaFX 21 |
| Database | PostgreSQL |
| Connection Pool | HikariCP |
| Serialization | Gson (JSON over TCP) |
| Password Hashing | BCrypt |
| Logging | SLF4J + Logback |

## Project Structure

```
JavaProgramming/
├── pom.xml                  (parent POM)
├── chat-common/             (shared models, protocol, utilities)
├── chat-server/             (server: socket, handlers, DB, services)
└── chat-client/             (JavaFX UI: controllers, components, FXML)
```

## Prerequisites

- **Java 17+** (JDK, not just JRE)
- **Maven 3.8+**
- **PostgreSQL 14+**

## Setup Instructions

### 1. Install Java 17

Download and install JDK 17 or later from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/).

Verify installation:
```bash
java -version
mvn -version
```

### 2. Install and Configure PostgreSQL

1. Download and install PostgreSQL from [postgresql.org](https://www.postgresql.org/download/).

2. Make sure the PostgreSQL service is running.

3. Create the database:
```bash
psql -U postgres -h localhost -c "CREATE DATABASE chat_app;"
```
> You will be prompted for the `postgres` user password you set during installation.

4. Update the database credentials in `chat-server/src/main/resources/server.properties`:
```properties
db.url=jdbc:postgresql://localhost:5432/chat_app
db.username=postgres
db.password=YOUR_PASSWORD_HERE
```
> Replace `YOUR_PASSWORD_HERE` with your actual PostgreSQL password.

> **Note:** The database tables are created automatically when the server starts for the first time. You do not need to run any SQL scripts manually.

### 3. Build the Project

From the project root directory:
```bash
mvn clean install
```

All three modules (chat-common, chat-server, chat-client) will be compiled and packaged.

### 4. Run the Server

```bash
cd chat-server
mvn exec:java -Dexec.mainClass=com.lqc.server.ChatServer
```

You should see:
```
Chat server started on port 9000
```

### 5. Run the Client

Open a **new terminal** and run:
```bash
cd chat-client
mvn javafx:run
```

A login window will appear. You can:
1. Click **Register** to create a new account
2. Log in with your credentials

> You can run multiple client instances in separate terminals to test multi-user chat.

## Configuration

### Server (`chat-server/src/main/resources/server.properties`)

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `9000` | TCP port the server listens on |
| `db.url` | `jdbc:postgresql://localhost:5432/chat_app` | JDBC connection URL |
| `db.username` | `postgres` | Database username |
| `db.password` | *(must be set)* | Database password |
| `db.pool.size` | `10` | HikariCP connection pool size |
| `file.storage.path` | `./uploads` | Directory for uploaded files |
| `file.max.size.mb` | `50` | Max file upload size in MB |

### Client (`chat-client/src/main/resources/client.properties`)

| Property | Default | Description |
|----------|---------|-------------|
| `server.host` | `localhost` | Server hostname |
| `server.port` | `9000` | Server port |

## Troubleshooting

### "database does not exist"
Run: `psql -U postgres -h localhost -c "CREATE DATABASE chat_app;"`

### "password authentication failed"
Check that `db.password` in `server.properties` matches your PostgreSQL password.

### Multiple PostgreSQL instances (WSL + Windows)
If you have both WSL and native Windows PostgreSQL, they may conflict on port 5432. The Java application connects to `localhost` which may resolve to a different instance than `psql`. Use `psql -h 127.0.0.1` to verify which instance has the `chat_app` database, or stop one of the instances.

### JavaFX "module not found"
Make sure you are using `mvn javafx:run` (not `java -jar`) to launch the client. The `javafx-maven-plugin` handles module path configuration automatically.

## Design Patterns

- **Command** — `RequestDispatcher` routes protocol messages to handlers
- **Observer** — `MessageListener` for real-time client notifications
- **MVC** — FXML (View) + Controllers + Models (chat-common)
- **Repository** — Database access layer per entity
- **Singleton** — `SessionManager` for thread-safe session tracking
