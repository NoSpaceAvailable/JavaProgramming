# Mini Discord — Ứng dụng Chat thời gian thực

> Đồ án môn **SE330.Q22 – Ngôn ngữ lập trình Java**
> Trường Đại học Công nghệ Thông tin, ĐHQG-HCM

Ứng dụng chat đa người dùng (multi-user) thời gian thực, lấy cảm hứng từ Discord, được xây dựng theo kiến trúc **client – server** với giao thức JSON truyền trên TCP. Hệ thống được tách thành nhiều module độc lập (microservices-style) theo mô hình Maven đa module, giúp phân tách rõ ràng giữa tầng giao thức dùng chung, tầng máy chủ và tầng giao diện.

---

## Thông tin nhóm

**Nhóm 26**

| STT | MSSV | Họ và tên |
|-----|----------|-----------|
| 1 | 23520350 | Ngô Phúc Dương |
| 2 | 23521789 | Hoàng Xuân Vinh |
| 3 | 23520197 | Lê Quốc Cường |

---

## Mục lục

1. [Giới thiệu](#1-giới-thiệu)
2. [Tính năng](#2-tính-năng)
3. [Công nghệ sử dụng](#3-công-nghệ-sử-dụng)
4. [Kiến trúc tổng quan](#4-kiến-trúc-tổng-quan)
5. [Cấu trúc thư mục](#5-cấu-trúc-thư-mục)
6. [Giao thức truyền tin](#6-giao-thức-truyền-tin)
7. [Cơ sở dữ liệu](#7-cơ-sở-dữ-liệu)
8. [Yêu cầu hệ thống](#8-yêu-cầu-hệ-thống)
9. [Hướng dẫn cài đặt](#9-hướng-dẫn-cài-đặt)
10. [Hướng dẫn chạy chương trình](#10-hướng-dẫn-chạy-chương-trình)
11. [Cấu hình](#11-cấu-hình)
12. [Các mẫu thiết kế (Design Patterns)](#12-các-mẫu-thiết-kế-design-patterns)
13. [Xử lý sự cố thường gặp](#13-xử-lý-sự-cố-thường-gặp)

---

## 1. Giới thiệu

**Mini Discord** mô phỏng các chức năng cốt lõi của một ứng dụng nhắn tin nhóm hiện đại: đăng ký/đăng nhập, trò chuyện theo phòng (group chat), nhắn tin riêng 1-1 (DM), gửi file, thả cảm xúc (reaction), hiển thị trạng thái online và lịch sử tin nhắn.

Hệ thống được chia làm 3 module Maven:

- **`chat-common`** — Tầng dùng chung: định nghĩa giao thức truyền tin, các lớp model (đối tượng dữ liệu) và tiện ích serialize JSON. Cả server lẫn client đều phụ thuộc module này để đảm bảo hai bên "nói cùng một ngôn ngữ".
- **`chat-server`** — Máy chủ: lắng nghe kết nối TCP, xử lý yêu cầu, truy xuất cơ sở dữ liệu và phát thông báo thời gian thực tới các client.
- **`chat-client`** — Ứng dụng giao diện JavaFX cho người dùng cuối.

---

## 2. Tính năng

- [x] **Đăng ký và đăng nhập** — mật khẩu được băm bằng BCrypt.
- [x] **Server cộng đồng + kênh (channel)** — tạo server, mã mời (invite code), tham gia bằng mã; mỗi server có nhiều kênh text (kiểu Discord).
- [x] **Phòng chat nhóm** — tạo, tham gia, rời phòng; hỗ trợ phòng công khai và riêng tư.
- [x] **Thêm/mời người vào phòng** — bất kỳ thành viên nào cũng có thể thêm người dùng khác vào phòng (kể cả phòng riêng tư).
- [x] **Xoá thành viên (kick)** — chủ phòng có thể xoá thành viên khỏi phòng.
- [x] **Typing indicator** — hiển thị "X đang gõ…" theo thời gian thực.
- [x] **Tìm kiếm tin nhắn** — tìm trong lịch sử của phòng hoặc cuộc trò chuyện riêng.
- [x] **Sửa / xoá tin nhắn** — người gửi có thể chỉnh sửa hoặc thu hồi tin nhắn của mình (cập nhật realtime, có nhãn "(edited)").
- [x] **Nhắn tin riêng (DM 1-1)** — định tuyến tin nhắn trực tiếp giữa hai người dùng.
- [x] **Truyền file** — tải lên/tải xuống theo từng khối (chunked) 64KB, kiểm tra toàn vẹn bằng SHA-256.
- [x] **Thả cảm xúc (emoji reaction)** trên từng tin nhắn.
- [x] **Hiển thị trạng thái** Online / Away / Offline theo thời gian thực.
- [x] **Lịch sử tin nhắn** có phân trang (pagination).
- [x] **Giao diện tối (dark theme)** lấy cảm hứng từ Discord.
- [x] **Ảnh đại diện (avatar)** và cập nhật hồ sơ cá nhân.
- [x] **Gửi ảnh GIF** thông qua tích hợp GIPHY API.

---

## 3. Công nghệ sử dụng

| Thành phần | Công nghệ |
|------------|-----------|
| Ngôn ngữ | Java 17 |
| Quản lý build | Maven (đa module) |
| Máy chủ | Java Socket + Đa luồng (Multithreading) |
| Giao diện | JavaFX 21 |
| Cơ sở dữ liệu | PostgreSQL |
| Connection Pool | HikariCP |
| Serialize dữ liệu | Gson (JSON trên TCP) |
| Băm mật khẩu | BCrypt |
| Ghi log | SLF4J + Logback |
| Kiểm thử | JUnit 5 |

---

## 4. Kiến trúc tổng quan

Hệ thống hoạt động theo mô hình **client – server**, giao tiếp qua TCP socket bằng các thông điệp JSON có tiền tố độ dài.

```
┌────────────────────┐         TCP (JSON length-prefixed)        ┌────────────────────┐
│    chat-client     │  ◄──────────────────────────────────────► │    chat-server     │
│   (JavaFX UI)      │                                            │  (Socket + Thread) │
│                    │   Request  ──────────────────────────►     │                    │
│  ServerConnection  │   Response/Notification  ◄────────────     │  RequestDispatcher │
└────────────────────┘                                            └─────────┬──────────┘
            ▲                                                                │
            │  cùng phụ thuộc                                                │ JDBC + HikariCP
            │                                                                ▼
     ┌──────┴───────┐                                              ┌────────────────────┐
     │ chat-common  │  (Protocol, Model, JsonUtil dùng chung)      │    PostgreSQL      │
     └──────────────┘                                              └────────────────────┘
```

**Luồng xử lý phía server:**

`ChatServer` (mở `ServerSocket`, mỗi client một luồng qua cached thread pool) → `ClientHandler` (đọc/ghi thông điệp có tiền tố độ dài 4 byte) → `RequestDispatcher` (định tuyến theo `MessageType`, chặn các yêu cầu chưa đăng nhập) → tầng **Service** (`AuthService`, `MessageService`, `RoomService`, `ReactionService`, `FileService`) → tầng **Repository** (truy xuất PostgreSQL).

`SessionManager` (singleton) quản lý các phiên đang online và thực hiện **broadcast** thông báo thời gian thực (tin nhắn mới, đổi trạng thái, reaction...) tới những client liên quan.

**Luồng xử lý phía client:**

`ServerConnection` (singleton) duy trì socket, gửi yêu cầu và lắng nghe thông báo bất đồng bộ trên một luồng riêng; mọi cập nhật giao diện được đưa về luồng JavaFX qua `Platform.runLater`. `SceneManager` điều phối chuyển màn hình giữa Login / Register / Main Chat. `MainChatController` là bộ điều khiển trung tâm của giao diện chat.

---

## 5. Cấu trúc thư mục

```
JavaProgramming/
├── pom.xml                  (POM cha — quản lý phiên bản & module)
├── README.md
├── TRACKING.md              (theo dõi tiến độ các giai đoạn)
├── Dockerfile               (build server thành fat-jar + image JRE để chạy)
├── docker-compose.yml       (dựng PostgreSQL + chat-server)
├── .dockerignore            (loại trừ target/, .git/, ... khỏi build context)
│
├── chat-common/             Module dùng chung
│   └── src/main/java/com/micord/common/
│       ├── model/           User, Room, Message, Reaction, FileAttachment, UserStatus
│       ├── protocol/        ProtocolMessage, MessageType
│       │   ├── request/     Các DTO yêu cầu (Login, CreateRoom, SendMessage, ...)
│       │   ├── response/    Các DTO phản hồi
│       │   └── notification/ Các DTO thông báo đẩy từ server
│       └── util/            JsonUtil, ProtocolConstants
│
├── chat-server/             Module máy chủ
│   └── src/main/
│       ├── java/com/micord/server/
│       │   ├── ChatServer.java       (điểm khởi động)
│       │   ├── ClientHandler.java    (xử lý kết nối từng client)
│       │   ├── handler/              (các handler xử lý yêu cầu + RequestDispatcher)
│       │   ├── service/              (logic nghiệp vụ)
│       │   ├── repository/           (truy xuất CSDL)
│       │   ├── manager/              (SessionManager)
│       │   ├── database/             (DatabaseConfig, DatabaseInitializer)
│       │   └── util/                 (ConfigUtil)
│       └── resources/        schema.sql, server.properties, logback.xml
│
└── chat-client/             Module giao diện (JavaFX)
    └── src/main/
        ├── java/com/micord/client/
        │   ├── ChatClientApp.java    (lớp Application của JavaFX)
        │   ├── Launcher.java         (điểm khởi động)
        │   ├── controller/           (Login, Register, MainChat)
        │   ├── net/                  (ServerConnection, MessageListener, FileTransferClient)
        │   ├── gif/                  (tích hợp GIPHY)
        │   └── util/                 (SceneManager)
        └── resources/        fxml/, css/dark-theme.css, emojis/, client.properties
```

---

## 6. Giao thức truyền tin

Mỗi thông điệp trên đường truyền gồm **tiền tố độ dài 4 byte (big-endian)** + phần thân JSON (UTF-8). Phần thân là một đối tượng `ProtocolMessage`:

| Trường | Kiểu | Ý nghĩa |
|--------|------|---------|
| `type` | `MessageType` | Loại thông điệp (xác định cách xử lý) |
| `payload` | `String` | Nội dung JSON của request/response/notification cụ thể |
| `requestId` | `String` | Mã UUID để ghép cặp yêu cầu – phản hồi |
| `timestamp` | `long` | Thời điểm tạo (Unix ms) |

- `MessageType` định nghĩa khoảng 50 loại thông điệp, chia nhóm: **Auth** (login/register), **Room** (tạo/tham gia/rời/liệt kê phòng), **Message** (gửi tin, lịch sử), **File** (upload/download theo chunk), **Reaction**, **Status**, **Profile/Avatar**, và **Notification** (server đẩy xuống client).
- `JsonUtil` dùng Gson để serialize, kèm **adapter tùy biến cho `LocalDateTime`** (lưu/đọc theo định dạng ISO-8601) nhằm tương thích với hệ thống module của Java 17.
- Các hằng số chung trong `ProtocolConstants`: cổng mặc định `9000`, kích thước khối file `65536` (64KB), giới hạn file `50MB`, kích thước trang lịch sử `50`.

---

## 7. Cơ sở dữ liệu

CSDL **PostgreSQL** gồm 7 bảng, được khởi tạo **tự động** khi server chạy lần đầu (từ `schema.sql`):

| Bảng | Vai trò |
|------|---------|
| `users` | Tài khoản người dùng (username, mật khẩu băm, display name, avatar, trạng thái) |
| `rooms` | Thông tin phòng chat (tên, mô tả, chủ phòng, công khai/riêng tư) |
| `room_members` | Quan hệ thành viên – phòng (kèm vai trò OWNER/MEMBER) |
| `messages` | Tin nhắn phòng và tin nhắn riêng (phân biệt qua `room_id`/`recipient_id`) |
| `file_attachments` | Metadata file đính kèm (đường dẫn, kích thước, MIME, checksum) |
| `reactions` | Cảm xúc thả trên tin nhắn (ràng buộc duy nhất theo message + user + emoji) |
| `dm_conversations` | Theo dõi các cuộc hội thoại riêng giữa hai người dùng |

> **Lưu ý:** Bạn **không** cần chạy script SQL thủ công. Các bảng được tạo tự động khi server khởi động lần đầu.

---

## 8. Yêu cầu hệ thống

- **Java 17 trở lên** (JDK, không phải chỉ JRE) — bắt buộc, để chạy client
- **Maven 3.8 trở lên** — bắt buộc
- **PostgreSQL 14 trở lên** — chỉ cần nếu chạy server thủ công (Cách B)
- **Docker + Docker Compose** — *(tùy chọn)* nếu chạy server bằng Docker (Cách A, khuyến nghị); khi đó không cần cài PostgreSQL riêng

Kiểm tra cài đặt:

```bash
java -version
mvn -version
```

---

## 9. Hướng dẫn cài đặt

### 9.1. Cài đặt Java 17

Tải JDK 17 trở lên từ [Adoptium](https://adoptium.net/) hoặc [Oracle](https://www.oracle.com/java/technologies/downloads/).

### 9.2. Cài đặt và cấu hình PostgreSQL

1. Tải và cài PostgreSQL từ [postgresql.org](https://www.postgresql.org/download/).
2. Đảm bảo dịch vụ PostgreSQL đang chạy.
3. Tạo cơ sở dữ liệu:

```bash
psql -U postgres -h localhost -c "CREATE DATABASE chat_app;"
```

> Bạn sẽ được yêu cầu nhập mật khẩu của tài khoản `postgres` đã đặt khi cài đặt.

4. Cập nhật thông tin kết nối CSDL trong `chat-server/src/main/resources/server.properties`:

```properties
db.url=jdbc:postgresql://localhost:5432/chat_app
db.username=postgres
db.password=MẬT_KHẨU_CỦA_BẠN
```

### 9.3. Build dự án

Tại thư mục gốc của dự án:

```bash
mvn clean install
```

Cả ba module (`chat-common`, `chat-server`, `chat-client`) sẽ được biên dịch và đóng gói.

---

## 10. Hướng dẫn chạy chương trình

Server có thể chạy theo **2 cách**: bằng Docker (khuyến nghị — dựng sẵn cả PostgreSQL) hoặc thủ công. **Client luôn chạy bên ngoài (native)** bằng `mvn javafx:run` để phân phát cho từng người dùng.

### 10.1. Cách A — Chạy Server + Database bằng Docker (khuyến nghị)

Cách này dựng đồng thời PostgreSQL và chat-server trong container, **không cần cài PostgreSQL** trên máy. Yêu cầu: đã cài Docker + Docker Compose.

Tại thư mục gốc dự án:

```bash
docker compose up --build -d
```

Lệnh này sẽ:

- Khởi tạo container PostgreSQL (`minidiscord-db`) với database `chat_app`.
- Build và chạy chat-server (`minidiscord-server`), tự tạo 7 bảng khi khởi động.
- Mở cổng `9000` ra máy host để client kết nối.

Kiểm tra trạng thái và log:

```bash
docker compose ps
docker compose logs -f server   # chờ tới khi thấy "Chat server started on port 9000"
```

Dừng / xoá:

```bash
docker compose down       # dừng, vẫn giữ dữ liệu (named volume)
docker compose down -v    # dừng và xoá sạch database + file đã upload
```

> Thông tin kết nối DB được truyền qua biến môi trường trong `docker-compose.yml`; server đọc qua `ConfigUtil` với thứ tự ưu tiên **biến môi trường > `server.properties`**. Cổng host của PostgreSQL được map ra `5433` để tránh xung đột nếu máy bạn đã chạy PostgreSQL ở cổng 5432.

### 10.2. Cách B — Chạy Server thủ công (không dùng Docker)

Yêu cầu đã cài và cấu hình PostgreSQL như mục [9.2](#92-cài-đặt-và-cấu-hình-postgresql).

```bash
cd chat-server
mvn exec:java "-Dexec.mainClass=com.micord.server.ChatServer" "-Duser.timezone=UTC"
```

Khi thành công, bạn sẽ thấy:

```
Chat server started on port 9000
```

### 10.3. Chạy Client (dùng cho cả hai cách trên)

Mở một **cửa sổ terminal mới**:

```bash
cd chat-client
mvn javafx:run
```

Mặc định client kết nối tới `localhost:9000`. Nếu server chạy ở máy khác (hoặc bạn phân phát cho người dùng khác **trong cùng mạng LAN**), chỉ cần trỏ tới IP của máy chủ:

```bash
mvn javafx:run -Dserver.host=<IP_MÁY_CHỦ> -Dserver.port=9000
```

> Ngoài tham số `-D`, có thể đặt biến môi trường `SERVER_HOST` / `SERVER_PORT`, hoặc sửa file `chat-client/src/main/resources/client.properties`. Thứ tự ưu tiên: tham số `-D` > biến môi trường > `client.properties` > mặc định.

Cửa sổ đăng nhập sẽ hiện ra. Bạn có thể:

1. Nhấn **Register** để tạo tài khoản mới.
2. Đăng nhập bằng tài khoản đã tạo.

> Có thể mở nhiều client ở các terminal/máy khác nhau để thử nghiệm chat đa người dùng.

---

## 11. Cấu hình

### 11.1. Server (`chat-server/src/main/resources/server.properties`)

| Thuộc tính | Mặc định | Mô tả |
|------------|----------|-------|
| `server.port` | `9000` | Cổng TCP server lắng nghe |
| `db.url` | `jdbc:postgresql://localhost:5432/chat_app` | Chuỗi kết nối JDBC |
| `db.username` | `postgres` | Tên đăng nhập CSDL |
| `db.password` | *(bắt buộc đặt)* | Mật khẩu CSDL |
| `db.pool.size` | `10` | Số kết nối trong pool HikariCP |
| `file.storage.path` | `./uploads` | Thư mục lưu file tải lên |
| `file.max.size.mb` | `50` | Dung lượng file tối đa (MB) |

### 11.2. Client (`chat-client/src/main/resources/client.properties`)

| Thuộc tính | Mặc định | Mô tả |
|------------|----------|-------|
| `server.host` | `localhost` | Tên máy chủ |
| `server.port` | `9000` | Cổng máy chủ |

---

## 12. Các mẫu thiết kế (Design Patterns)

- **Command** — `RequestDispatcher` định tuyến thông điệp giao thức tới handler tương ứng.
- **Observer** — `MessageListener` nhận thông báo thời gian thực ở phía client.
- **MVC** — FXML (View) + Controller + Model (`chat-common`).
- **Repository** — tầng truy xuất CSDL riêng cho từng thực thể.
- **Singleton** — `SessionManager` (server) và `ServerConnection` (client) quản lý trạng thái dùng chung an toàn đa luồng.

---

## 13. Xử lý sự cố thường gặp

### "database does not exist"

Chạy lệnh: `psql -U postgres -h localhost -c "CREATE DATABASE chat_app;"`

### "password authentication failed"

Kiểm tra `db.password` trong `server.properties` có khớp với mật khẩu PostgreSQL của bạn không.

### Có nhiều phiên bản PostgreSQL (WSL + Windows)

Nếu bạn cài cả PostgreSQL trên WSL lẫn Windows, chúng có thể xung đột ở cổng 5432. Ứng dụng Java kết nối tới `localhost`, có thể trỏ tới phiên bản khác với khi bạn dùng `psql`. Hãy dùng `psql -h 127.0.0.1` để xác định phiên bản nào đang chứa CSDL `chat_app`, hoặc dừng một phiên bản.

### JavaFX báo "module not found"

Hãy chắc chắn dùng `mvn javafx:run` (không dùng `java -jar`) để chạy client. Plugin `javafx-maven-plugin` sẽ tự cấu hình module path.
