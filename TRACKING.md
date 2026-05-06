# Mini Discord - Project Progress Tracking

## Overview
| Phase | Description | Status | Est. Days |
|-------|-------------|--------|-----------|
| 1 | Project Setup + Basic Connectivity | DONE | 3-4 |
| 2 | Authentication + Database | DONE | 3-4 |
| 3 | Room Management + Group Chat | DONE | 4-5 |
| 4 | Private Messaging + User Status | IN PROGRESS | 3-4 |
| 5 | File Transfer | DONE | 3-4 |
| 6 | Emoji Reactions | DONE | 2-3 |
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
- [x] Implement RoomRepository + MessageRepository
- [x] Implement RoomService + MessageService
- [x] Implement SessionManager (RoomManager not needed — broadcast via SessionManager + room_members table)
- [x] Implement CreateRoom/JoinRoom/LeaveRoom/SendMessage/History handlers
- [x] Build main-chat.fxml (3-panel layout)
- [x] Implement MainChatController
- [x] Inline message bubble + room list cell rendering (no separate components)
- [x] Implement real-time broadcasting
- [ ] Verify: 2+ clients chat in rooms in real-time (manual test)

## Phase 4: Private Messaging + User Status
- [x] Implement private message routing (PrivateMessageHandler, DM history)
- [x] Add DM sidebar section in UI
- [x] User picker for starting new DMs (LIST_USERS_REQUEST)
- [ ] Implement StatusUpdateRequest/StatusHandler
- [ ] Implement StatusIndicator + UserListCell components (basic dot indicator added)
- [ ] Add status dropdown in sidebar
- [x] Auto-mark OFFLINE on disconnect (already in ClientHandler.disconnect)
- [ ] Verify: DMs + status updates work in real-time (manual test)

## Phase 5: File Transfer
- [x] FileService (disk storage under file.storage.path, tmp + final dirs, SHA-256)
- [x] FileAttachmentRepository + FileService
- [x] FileUploadStart/Chunk/Complete + FileDownload handlers
- [x] FileTransferClient drives chunked upload/download with progress callbacks
- [x] Inline file message rendering with download button (no separate component)
- [x] FileChooser for upload/download
- [ ] Verify: File upload + download works (manual test)

## Phase 6: Emoji Reactions
- [x] ReactionRepository + ReactionService + Add/Remove handlers
- [x] Inline emoji palette via right-click context menu (👍 ❤ 😂 🎉 😮 😢 🔥 👀)
- [x] Reaction context menu on messages
- [x] Reaction badges with click-to-toggle
- [ ] Verify: Reactions persist and display correctly (manual test)

## Phase 7: Polish + Testing
- [ ] Graceful disconnect handling
- [ ] Input validation
- [ ] Unread message badges
- [ ] Formatted timestamps
- [ ] System messages
- [ ] Unit tests
- [ ] Integration tests
- [ ] Verify: Full end-to-end with 3+ clients
