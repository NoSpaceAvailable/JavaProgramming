# Mini Discord - Project Progress Tracking

## Overview
| Phase | Description | Status | Est. Days |
|-------|-------------|--------|-----------|
| 1 | Project Setup + Basic Connectivity | DONE | 3-4 |
| 2 | Authentication + Database | DONE | 3-4 |
| 3 | Room Management + Group Chat | NOT STARTED | 4-5 |
| 4 | Private Messaging + User Status | NOT STARTED | 3-4 |
| 5 | File Transfer | NOT STARTED | 3-4 |
| 6 | Emoji Reactions | NOT STARTED | 2-3 |
| 7 | Polish + Testing | NOT STARTED | 3-4 |

---

## Phase 1: Project Setup + Basic Connectivity
- [x] Convert pom.xml to parent POM (packaging=pom)
- [x] Create chat-common module + pom.xml
- [x] Create chat-server module + pom.xml
- [x] Create chat-client module + pom.xml
- [x] Implement ProtocolMessage + MessageType in chat-common
- [x] Implement JsonUtil in chat-common
- [x] Implement ChatServer + ClientHandler in chat-server
- [x] Implement ServerConnection + MessageListener in chat-client
- [x] Create minimal JavaFX ChatClientApp
- [x] Verify: `mvn clean install` succeeds
- [x] Verify: Server starts and listens on port 9000

## Phase 2: Authentication + Database
- [x] Create PostgreSQL database + schema.sql (all 7 tables)
- [x] Implement DatabaseConfig (HikariCP) + DatabaseInitializer
- [x] Implement UserRepository + RoomRepository
- [x] Implement AuthService (register/login with BCrypt)
- [x] Implement LoginHandler + RegisterHandler + RequestDispatcher
- [x] Implement SessionManager singleton
- [x] Create login.fxml + LoginController
- [x] Create register.fxml + RegisterController
- [x] Apply dark-theme.css (Discord-inspired dark theme)
- [x] Implement SceneManager for screen switching
- [x] Implement ConfigUtil for server.properties
- [x] Verify: Server initializes DB schema on startup
- [x] Verify: Client shows login screen with dark theme

## Phase 3: Room Management + Group Chat
- [ ] Implement RoomRepository + MessageRepository
- [ ] Implement RoomService + MessageService
- [ ] Implement SessionManager + RoomManager singletons
- [ ] Implement CreateRoom/JoinRoom/LeaveRoom/SendMessage/History handlers
- [ ] Build main-chat.fxml (3-panel layout)
- [ ] Implement MainChatController
- [ ] Implement RoomListCell + MessageBubble components
- [ ] Implement real-time broadcasting
- [ ] Verify: 2+ clients chat in rooms in real-time

## Phase 4: Private Messaging + User Status
- [ ] Implement private message routing
- [ ] Add DM sidebar section in UI
- [ ] Implement StatusUpdateRequest/StatusHandler
- [ ] Implement StatusIndicator + UserListCell components
- [ ] Add status dropdown in sidebar
- [ ] Auto-mark OFFLINE on disconnect
- [ ] Verify: DMs + status updates work in real-time

## Phase 5: File Transfer
- [ ] Implement FileManager (disk storage)
- [ ] Implement FileRepository + FileService
- [ ] Implement FileTransferHandler (server) + FileTransferClient (client)
- [ ] Implement FilePreview component
- [ ] Add FileChooser for upload/download
- [ ] Verify: File upload + download works

## Phase 6: Emoji Reactions
- [ ] Implement ReactionRepository + ReactionService + ReactionHandler
- [ ] Create EmojiPicker component + EmojiData
- [ ] Add reaction context menu on messages
- [ ] Display reaction badges
- [ ] Verify: Reactions persist and display correctly

## Phase 7: Polish + Testing
- [ ] Graceful disconnect handling
- [ ] Input validation
- [ ] Unread message badges
- [ ] Formatted timestamps
- [ ] System messages
- [ ] Unit tests
- [ ] Integration tests
- [ ] Verify: Full end-to-end with 3+ clients
