package com.lqc.client.controller;

import com.lqc.client.net.FileTransferClient;
import com.lqc.client.net.MessageListener;
import com.lqc.client.net.ServerConnection;
import com.lqc.client.util.SceneManager;
import com.lqc.common.model.FileAttachment;
import com.lqc.common.model.Message;
import com.lqc.common.model.Reaction;
import com.lqc.common.model.Room;
import com.lqc.common.model.User;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.notification.NewMessageNotification;
import com.lqc.common.protocol.notification.ReactionNotification;
import com.lqc.common.protocol.notification.UserJoinedNotification;
import com.lqc.common.protocol.notification.UserLeftNotification;
import com.lqc.common.protocol.request.*;
import com.lqc.common.protocol.response.*;
import com.lqc.common.util.JsonUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainChatController implements MessageListener {

    @FXML private Label userBadge;
    @FXML private Label connectionLabel;
    @FXML private Label conversationTitle;
    @FXML private Label membersTitle;
    @FXML private Button leaveButton;
    @FXML private ListView<Room> roomListView;
    @FXML private ListView<DmEntry> dmListView;
    @FXML private ListView<User> memberListView;
    @FXML private VBox messagesBox;
    @FXML private VBox membersPanel;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextField messageInput;
    @FXML private Button attachButton;
    @FXML private Label uploadStatusLabel;

    private final ServerConnection connection = ServerConnection.getInstance();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private long currentUserId;
    private String currentDisplayName;

    private final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private final ObservableList<DmEntry> dms = FXCollections.observableArrayList();
    private final ObservableList<User> members = FXCollections.observableArrayList();
    // Cache of every user we've seen, used to render DM display names.
    private final Map<Long, User> knownUsers = new LinkedHashMap<>();
    // messageId -> rendered bubble container (so we can update reaction badges in place).
    private final Map<Long, MessageBubble> bubbles = new LinkedHashMap<>();

    private static final List<String> EMOJI_PALETTE = List.of(
            "👍", "❤", "😂", "🎉", "😮", "😢", "🔥", "👀");

    private Conversation active;

    @FXML
    public void initialize() {
        connection.addListener(this);
        FileTransferClient.getInstance().ensureRegistered();

        roomListView.setItems(rooms);
        roomListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "# " + item.getName());
            }
        });
        roomListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                dmListView.getSelectionModel().clearSelection();
                openRoom(newV);
            }
        });

        dmListView.setItems(dms);
        dmListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DmEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "@ " + item.displayName);
            }
        });
        dmListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                roomListView.getSelectionModel().clearSelection();
                openDm(newV);
            }
        });

        memberListView.setItems(members);
        memberListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    return;
                }
                String dot = switch (user.getStatus()) {
                    case ONLINE -> "● ";
                    case AWAY -> "◐ ";
                    case OFFLINE -> "○ ";
                };
                setText(dot + user.getDisplayName());
            }
        });
        memberListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                User u = memberListView.getSelectionModel().getSelectedItem();
                if (u != null && u.getId() != currentUserId) startDm(u);
            }
        });
    }

    public void initWithUser(long userId, String displayName, List<Room> initialRooms) {
        this.currentUserId = userId;
        this.currentDisplayName = displayName;
        userBadge.setText("@" + displayName);
        if (initialRooms != null) rooms.setAll(initialRooms);
        attachButton.setDisable(true);
        showSystemPlaceholder("Select a room from the left or start a DM.");
        // Also fetch the user list eagerly so the DM picker has data ready.
        connection.send(JsonUtil.wrap(MessageType.LIST_USERS_REQUEST, new Object()));
    }

    // ---- UI handlers ----

    @FXML
    private void handleCreateRoom() {
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setTitle("Create Room");
        nameDialog.setHeaderText("Create a new chat room");
        nameDialog.setContentText("Room name:");
        Optional<String> nameOpt = nameDialog.showAndWait();
        if (nameOpt.isEmpty() || nameOpt.get().trim().isEmpty()) return;

        TextInputDialog descDialog = new TextInputDialog();
        descDialog.setTitle("Create Room");
        descDialog.setHeaderText("Room description (optional)");
        descDialog.setContentText("Description:");
        String desc = descDialog.showAndWait().orElse("");

        connection.send(JsonUtil.wrap(MessageType.CREATE_ROOM_REQUEST,
                new CreateRoomRequest(nameOpt.get().trim(), desc.trim(), false)));
    }

    @FXML
    private void handleBrowseRooms() {
        connection.send(JsonUtil.wrap(MessageType.LIST_PUBLIC_ROOMS_REQUEST, new Object()));
    }

    @FXML
    private void handleLeaveRoom() {
        if (active == null || active.kind != Conversation.Kind.ROOM) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Leave room \"" + active.title + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b ->
                connection.send(JsonUtil.wrap(MessageType.LEAVE_ROOM_REQUEST,
                        new LeaveRoomRequest(active.id))));
    }

    @FXML
    private void handleNewDm() {
        if (knownUsers.isEmpty()) {
            connection.send(JsonUtil.wrap(MessageType.LIST_USERS_REQUEST, new Object()));
            new Alert(Alert.AlertType.INFORMATION, "Loading user list, try again in a moment.").showAndWait();
            return;
        }
        Map<String, User> labelToUser = new LinkedHashMap<>();
        for (User u : knownUsers.values()) {
            String dot = switch (u.getStatus()) {
                case ONLINE -> "● ";
                case AWAY -> "◐ ";
                case OFFLINE -> "○ ";
            };
            labelToUser.put(dot + u.getDisplayName() + " (@" + u.getUsername() + ")", u);
        }
        List<String> labels = new ArrayList<>(labelToUser.keySet());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle("New Direct Message");
        dialog.setHeaderText("Pick a user to message");
        dialog.setContentText("User:");
        dialog.showAndWait().map(labelToUser::get).ifPresent(this::startDm);
    }

    @FXML
    private void handleSendMessage() {
        String text = messageInput.getText();
        if (text == null || text.trim().isEmpty() || active == null) return;
        text = text.trim();
        if (active.kind == Conversation.Kind.ROOM) {
            connection.send(JsonUtil.wrap(MessageType.SEND_MESSAGE_REQUEST,
                    new SendMessageRequest(active.id, text)));
        } else {
            connection.send(JsonUtil.wrap(MessageType.PRIVATE_MESSAGE_REQUEST,
                    new PrivateMessageRequest(active.id, text)));
        }
        messageInput.clear();
    }

    @FXML
    private void handleAttachFile() {
        if (active == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Send file");
        java.io.File file = chooser.showOpenDialog(SceneManager.getPrimaryStage());
        if (file == null) return;

        Path path = file.toPath();
        long size;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Cannot read file: " + e.getMessage());
            return;
        }
        if (size > 50L * 1024 * 1024) {
            showAlert(Alert.AlertType.ERROR, "File exceeds 50 MB limit.");
            return;
        }
        String mime;
        try {
            mime = Files.probeContentType(path);
        } catch (IOException e) {
            mime = URLConnection.guessContentTypeFromName(file.getName());
        }
        if (mime == null) mime = "application/octet-stream";

        Long roomId = active.kind == Conversation.Kind.ROOM ? active.id : null;
        Long recipientId = active.kind == Conversation.Kind.DM ? active.id : null;

        showUploadStatus("Uploading " + file.getName() + "…");
        attachButton.setDisable(true);
        FileTransferClient.getInstance().upload(path, roomId, recipientId, mime,
                new FileTransferClient.UploadCallback() {
                    @Override
                    public void onProgress(long sent, long total) {
                        int pct = total <= 0 ? 0 : (int) ((sent * 100) / total);
                        showUploadStatus("Uploading " + file.getName() + "… " + pct + "%");
                    }
                    @Override
                    public void onComplete(long messageId, long attachmentId) {
                        hideUploadStatus();
                        if (active != null) attachButton.setDisable(false);
                    }
                    @Override
                    public void onError(String msg) {
                        hideUploadStatus();
                        if (active != null) attachButton.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Upload failed: " + msg);
                    }
                });
    }

    @FXML
    private void handleLogout() {
        connection.removeListener(this);
        connection.disconnect();
        SceneManager.switchTo("login");
    }

    // ---- Conversation switching ----

    private void openRoom(Room room) {
        active = new Conversation(Conversation.Kind.ROOM, room.getId(), room.getName());
        conversationTitle.setText("# " + room.getName());
        leaveButton.setVisible(true);
        leaveButton.setManaged(true);
        membersPanel.setVisible(true);
        membersPanel.setManaged(true);
        membersTitle.setText("MEMBERS");
        members.clear();
        messageInput.setDisable(false);
        attachButton.setDisable(false);
        messageInput.setPromptText("Message #" + room.getName());
        clearMessages();
        connection.send(JsonUtil.wrap(MessageType.GET_HISTORY_REQUEST,
                new GetHistoryRequest(room.getId(), 0, 50)));
        connection.send(JsonUtil.wrap(MessageType.ROOM_MEMBERS_REQUEST,
                new RoomMembersRequest(room.getId())));
    }

    private void openDm(DmEntry dm) {
        active = new Conversation(Conversation.Kind.DM, dm.userId, dm.displayName);
        conversationTitle.setText("@ " + dm.displayName);
        leaveButton.setVisible(false);
        leaveButton.setManaged(false);
        membersPanel.setVisible(false);
        membersPanel.setManaged(false);
        messageInput.setDisable(false);
        attachButton.setDisable(false);
        messageInput.setPromptText("Message @" + dm.displayName);
        clearMessages();
        connection.send(JsonUtil.wrap(MessageType.DM_HISTORY_REQUEST,
                new DmHistoryRequest(dm.userId, 0, 50)));
    }

    private void clearMessages() {
        messagesBox.getChildren().clear();
        bubbles.clear();
    }

    private void startDm(User user) {
        DmEntry existing = findDm(user.getId());
        if (existing == null) {
            existing = new DmEntry(user.getId(), user.getDisplayName());
            dms.add(existing);
        }
        dmListView.getSelectionModel().select(existing);
    }

    private DmEntry findDm(long userId) {
        for (DmEntry e : dms) if (e.userId == userId) return e;
        return null;
    }

    // ---- Server message dispatch ----

    @Override
    public void onMessageReceived(ProtocolMessage message) {
        switch (message.getType()) {
            case CREATE_ROOM_RESPONSE -> onCreateRoomResponse(message);
            case JOIN_ROOM_RESPONSE -> onJoinRoomResponse(message);
            case LEAVE_ROOM_RESPONSE -> onLeaveRoomResponse(message);
            case LIST_ROOMS_RESPONSE -> onRoomListResponse(message, false);
            case LIST_PUBLIC_ROOMS_RESPONSE -> onRoomListResponse(message, true);
            case ROOM_MEMBERS_RESPONSE -> onRoomMembersResponse(message);
            case LIST_USERS_RESPONSE -> onUserListResponse(message);
            case GET_HISTORY_RESPONSE -> onHistoryResponse(message);
            case NEW_MESSAGE_NOTIFICATION -> onNewMessage(message);
            case REACTION_NOTIFICATION -> onReactionNotification(message);
            case USER_JOINED_NOTIFICATION -> onUserJoined(message);
            case USER_LEFT_NOTIFICATION -> onUserLeft(message);
            case ERROR_RESPONSE -> onError(message);
            case SEND_MESSAGE_RESPONSE, PRIVATE_MESSAGE_RESPONSE -> {
                SendMessageResponse r = JsonUtil.fromJson(message.getPayload(), SendMessageResponse.class);
                if (!r.isSuccess()) showAlert(Alert.AlertType.ERROR, r.getMessage());
            }
            default -> { /* ignore */ }
        }
    }

    private void onCreateRoomResponse(ProtocolMessage m) {
        CreateRoomResponse r = JsonUtil.fromJson(m.getPayload(), CreateRoomResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
            return;
        }
        if (r.getRoom() != null) {
            rooms.add(r.getRoom());
            roomListView.getSelectionModel().select(r.getRoom());
        }
    }

    private void onJoinRoomResponse(ProtocolMessage m) {
        JoinRoomResponse r = JsonUtil.fromJson(m.getPayload(), JoinRoomResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
            return;
        }
        Room room = r.getRoom();
        if (room == null) return;
        if (rooms.stream().noneMatch(rr -> rr.getId() == room.getId())) {
            rooms.add(room);
        }
        roomListView.getSelectionModel().select(room);
    }

    private void onLeaveRoomResponse(ProtocolMessage m) {
        LeaveRoomResponse r = JsonUtil.fromJson(m.getPayload(), LeaveRoomResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
            return;
        }
        rooms.removeIf(rr -> rr.getId() == r.getRoomId());
        if (active != null && active.kind == Conversation.Kind.ROOM && active.id == r.getRoomId()) {
            active = null;
            conversationTitle.setText("Select a room or DM");
            leaveButton.setVisible(false);
            leaveButton.setManaged(false);
            clearMessages();
            messageInput.setDisable(true);
            attachButton.setDisable(true);
            members.clear();
        }
    }

    private void onRoomListResponse(ProtocolMessage m, boolean isBrowse) {
        RoomListResponse r = JsonUtil.fromJson(m.getPayload(), RoomListResponse.class);
        List<Room> list = r.getRooms() == null ? List.of() : r.getRooms();
        if (!isBrowse) {
            rooms.setAll(list);
            return;
        }
        // Browse public rooms: show join dialog.
        List<Room> joinable = new ArrayList<>();
        for (Room candidate : list) {
            if (rooms.stream().noneMatch(rr -> rr.getId() == candidate.getId())) {
                joinable.add(candidate);
            }
        }
        if (joinable.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No other public rooms to join.");
            return;
        }
        ChoiceDialog<Room> dialog = new ChoiceDialog<>(joinable.get(0), joinable);
        dialog.setTitle("Join Room");
        dialog.setHeaderText("Public rooms");
        dialog.setContentText("Pick a room to join:");
        dialog.showAndWait().ifPresent(room ->
                connection.send(JsonUtil.wrap(MessageType.JOIN_ROOM_REQUEST,
                        new JoinRoomRequest(room.getId()))));
    }

    private void onRoomMembersResponse(ProtocolMessage m) {
        RoomMembersResponse r = JsonUtil.fromJson(m.getPayload(), RoomMembersResponse.class);
        if (active == null || active.kind != Conversation.Kind.ROOM || active.id != r.getRoomId()) return;
        if (r.getMembers() != null) {
            members.setAll(r.getMembers());
            for (User u : r.getMembers()) knownUsers.put(u.getId(), u);
            membersTitle.setText("MEMBERS — " + r.getMembers().size());
        }
    }

    private void onUserListResponse(ProtocolMessage m) {
        UserListResponse r = JsonUtil.fromJson(m.getPayload(), UserListResponse.class);
        if (r.getUsers() != null) {
            knownUsers.clear();
            for (User u : r.getUsers()) knownUsers.put(u.getId(), u);
        }
    }

    private void onHistoryResponse(ProtocolMessage m) {
        MessageHistoryResponse r = JsonUtil.fromJson(m.getPayload(), MessageHistoryResponse.class);
        if (active == null) return;
        boolean matchesRoom = active.kind == Conversation.Kind.ROOM && r.getRoomId() == active.id;
        boolean matchesDm = active.kind == Conversation.Kind.DM && r.getRoomId() == 0;
        if (!matchesRoom && !matchesDm) return;
        clearMessages();
        if (r.getMessages() != null) {
            for (Message msg : r.getMessages()) {
                MessageBubble bubble = renderMessage(msg);
                messagesBox.getChildren().add(bubble.root);
                bubbles.put(msg.getId(), bubble);
            }
        }
        scrollToBottom();
    }

    private void onNewMessage(ProtocolMessage m) {
        NewMessageNotification n = JsonUtil.fromJson(m.getPayload(), NewMessageNotification.class);
        boolean isRoom = n.getRoomId() != null && n.getRoomId() > 0;
        if (isRoom) {
            if (active != null && active.kind == Conversation.Kind.ROOM && active.id == n.getRoomId()) {
                appendAndScroll(n);
            }
            return;
        }
        // DM: figure out the peer (the other party).
        long peerId = n.getSenderId() == currentUserId
                ? (n.getRecipientId() != null ? n.getRecipientId() : 0)
                : n.getSenderId();
        if (peerId == 0) return;
        DmEntry entry = findDm(peerId);
        if (entry == null) {
            String name = knownUsers.containsKey(peerId)
                    ? knownUsers.get(peerId).getDisplayName()
                    : (n.getSenderId() == currentUserId ? "User #" + peerId : n.getSenderName());
            entry = new DmEntry(peerId, name);
            dms.add(entry);
        }
        if (active != null && active.kind == Conversation.Kind.DM && active.id == peerId) {
            appendAndScroll(n);
        }
    }

    private void onUserJoined(ProtocolMessage m) {
        UserJoinedNotification n = JsonUtil.fromJson(m.getPayload(), UserJoinedNotification.class);
        if (active != null && active.kind == Conversation.Kind.ROOM && active.id == n.getRoomId()) {
            connection.send(JsonUtil.wrap(MessageType.ROOM_MEMBERS_REQUEST,
                    new RoomMembersRequest(n.getRoomId())));
            messagesBox.getChildren().add(renderSystem(n.getDisplayName() + " joined."));
            scrollToBottom();
        }
    }

    private void onUserLeft(ProtocolMessage m) {
        UserLeftNotification n = JsonUtil.fromJson(m.getPayload(), UserLeftNotification.class);
        if (active != null && active.kind == Conversation.Kind.ROOM && active.id == n.getRoomId()) {
            members.removeIf(u -> u.getId() == n.getUserId());
            membersTitle.setText("MEMBERS — " + members.size());
            messagesBox.getChildren().add(renderSystem(n.getDisplayName() + " left."));
            scrollToBottom();
        }
    }

    private void onReactionNotification(ProtocolMessage m) {
        ReactionNotification n = JsonUtil.fromJson(m.getPayload(), ReactionNotification.class);
        MessageBubble bubble = bubbles.get(n.getMessageId());
        if (bubble == null) return;
        bubble.applyReaction(n);
    }

    private void onError(ProtocolMessage m) {
        ErrorResponse r = JsonUtil.fromJson(m.getPayload(), ErrorResponse.class);
        showAlert(Alert.AlertType.ERROR, r.getMessage());
    }

    // ---- Rendering helpers ----

    private void appendAndScroll(NewMessageNotification n) {
        Message m = new Message();
        m.setId(n.getMessageId());
        m.setSenderId(n.getSenderId());
        m.setSenderName(n.getSenderName());
        m.setRoomId(n.getRoomId());
        m.setRecipientId(n.getRecipientId());
        m.setContent(n.getContent());
        if ("FILE".equalsIgnoreCase(n.getMessageType())) {
            m.setMessageType(Message.MessageType.FILE);
            if (n.getFileAttachmentId() != null) {
                FileAttachment a = new FileAttachment();
                a.setId(n.getFileAttachmentId());
                a.setMessageId(n.getMessageId());
                a.setFileName(n.getFileName());
                a.setFileSize(n.getFileSize() != null ? n.getFileSize() : 0);
                a.setMimeType(n.getMimeType());
                m.setAttachment(a);
            }
        } else {
            m.setMessageType(Message.MessageType.TEXT);
        }
        m.setCreatedAt(java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(n.getTimestamp()), ZoneId.systemDefault()));
        MessageBubble bubble = renderMessage(m);
        messagesBox.getChildren().add(bubble.root);
        bubbles.put(m.getId(), bubble);
        scrollToBottom();
    }

    private MessageBubble renderMessage(Message m) {
        long timestamp = m.getCreatedAt() != null
                ? m.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();

        Label nameLabel = new Label(m.getSenderName() == null ? "Unknown" : m.getSenderName());
        nameLabel.getStyleClass().add("sender-name");
        nameLabel.setStyle(m.getSenderId() == currentUserId
                ? "-fx-text-fill: #7289da;"
                : "-fx-text-fill: #ffffff;");
        Label timeLabel = new Label(formatTime(timestamp));
        timeLabel.getStyleClass().add("timestamp");

        HBox header = new HBox(8, nameLabel, timeLabel);
        header.setAlignment(Pos.BASELINE_LEFT);

        Node body;
        if (m.getAttachment() != null) {
            body = renderAttachment(m.getAttachment());
        } else {
            Label text = new Label(m.getContent() == null ? "" : m.getContent());
            text.getStyleClass().add("message-content");
            text.setWrapText(true);
            text.setMaxWidth(Double.MAX_VALUE);
            body = text;
        }

        FlowPane reactionRow = new FlowPane(6, 4);
        reactionRow.setVisible(false);
        reactionRow.setManaged(false);

        VBox container = new VBox(2, header, body, reactionRow);
        container.getStyleClass().add("message-bubble");

        MessageBubble bubble = new MessageBubble(container, reactionRow, m.getId());
        bubble.replaceReactions(m.getReactions());

        container.setOnMouseClicked(ev -> {
            if (ev.getButton() == MouseButton.SECONDARY) {
                showReactionMenu(bubble, ev.getScreenX(), ev.getScreenY());
            }
        });
        return bubble;
    }

    private Node renderAttachment(FileAttachment att) {
        Label fileLabel = new Label("📎 " + att.getFileName() + " (" + formatBytes(att.getFileSize()) + ")");
        fileLabel.setStyle("-fx-text-fill: #00b0f4; -fx-font-weight: bold;");
        Button download = new Button("Download");
        download.getStyleClass().add("button-link");
        download.setOnAction(e -> startDownload(att));
        HBox box = new HBox(10, fileLabel, download);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("message-content");
        box.setStyle("-fx-background-color: #2f3136; -fx-background-radius: 6; -fx-padding: 8 12;");
        return box;
    }

    private void showReactionMenu(MessageBubble bubble, double x, double y) {
        ContextMenu menu = new ContextMenu();
        for (String emoji : EMOJI_PALETTE) {
            MenuItem item = new MenuItem(emoji);
            item.setOnAction(e -> toggleReaction(bubble.messageId, emoji));
            menu.getItems().add(item);
        }
        menu.show(bubble.root, x, y);
    }

    private void toggleReaction(long messageId, String emoji) {
        MessageBubble bubble = bubbles.get(messageId);
        boolean userHas = bubble != null && bubble.userHasReaction(currentUserId, emoji);
        MessageType type = userHas ? MessageType.REMOVE_REACTION_REQUEST : MessageType.ADD_REACTION_REQUEST;
        connection.send(JsonUtil.wrap(type, new EmojiReactionRequest(messageId, emoji)));
    }

    private void startDownload(FileAttachment att) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save file");
        chooser.setInitialFileName(att.getFileName());
        java.io.File saveFile = chooser.showSaveDialog(SceneManager.getPrimaryStage());
        if (saveFile == null) return;
        Path savePath = saveFile.toPath();
        showUploadStatus("Downloading " + att.getFileName() + "…");
        FileTransferClient.getInstance().download(att.getId(), att.getFileName(), savePath,
                new FileTransferClient.DownloadCallback() {
                    @Override
                    public void onProgress(long received, long total) {
                        int pct = total <= 0 ? 0 : (int) ((received * 100) / total);
                        showUploadStatus("Downloading " + att.getFileName() + "… " + pct + "%");
                    }
                    @Override
                    public void onComplete(Path savedTo) {
                        hideUploadStatus();
                        showAlert(Alert.AlertType.INFORMATION, "Saved to " + savedTo);
                    }
                    @Override
                    public void onError(String msg) {
                        hideUploadStatus();
                        showAlert(Alert.AlertType.ERROR, "Download failed: " + msg);
                    }
                });
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void showUploadStatus(String text) {
        uploadStatusLabel.setText(text);
        uploadStatusLabel.setVisible(true);
        uploadStatusLabel.setManaged(true);
    }

    private void hideUploadStatus() {
        uploadStatusLabel.setVisible(false);
        uploadStatusLabel.setManaged(false);
    }

    private VBox renderSystem(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #72767d; -fx-font-style: italic; -fx-font-size: 12px;");
        VBox box = new VBox(l);
        box.getStyleClass().add("message-bubble");
        return box;
    }

    private void showSystemPlaceholder(String text) {
        messagesBox.getChildren().clear();
        Region top = new Region();
        top.setMinHeight(24);
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #72767d; -fx-font-size: 14px;");
        VBox box = new VBox(8, top, l);
        box.setAlignment(Pos.CENTER);
        messagesBox.getChildren().add(box);
    }

    private String formatTime(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(TIME_FMT);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    private void showAlert(Alert.AlertType type, String text) {
        Alert a = new Alert(type, text);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // ---- Helper types ----

    private static final class Conversation {
        enum Kind { ROOM, DM }
        final Kind kind;
        final long id;
        final String title;
        Conversation(Kind kind, long id, String title) {
            this.kind = kind;
            this.id = id;
            this.title = title;
        }
    }

    private final class MessageBubble {
        final VBox root;
        final FlowPane reactionRow;
        final long messageId;
        // emoji -> set of userIds who reacted with it
        final Map<String, java.util.LinkedHashSet<Long>> reactions = new LinkedHashMap<>();
        // emoji -> the badge node currently rendered for it
        final Map<String, Button> badges = new LinkedHashMap<>();

        MessageBubble(VBox root, FlowPane reactionRow, long messageId) {
            this.root = root;
            this.reactionRow = reactionRow;
            this.messageId = messageId;
        }

        void replaceReactions(List<Reaction> seed) {
            reactions.clear();
            badges.clear();
            reactionRow.getChildren().clear();
            if (seed == null) {
                refreshVisibility();
                return;
            }
            for (Reaction r : seed) {
                reactions.computeIfAbsent(r.getEmoji(),
                        k -> new java.util.LinkedHashSet<>()).add(r.getUserId());
            }
            for (var entry : reactions.entrySet()) {
                renderBadge(entry.getKey(), entry.getValue());
            }
            refreshVisibility();
        }

        void applyReaction(ReactionNotification n) {
            var set = reactions.computeIfAbsent(n.getEmoji(),
                    k -> new java.util.LinkedHashSet<>());
            if (n.isAdded()) set.add(n.getUserId());
            else set.remove(n.getUserId());
            if (set.isEmpty()) {
                reactions.remove(n.getEmoji());
                Button removed = badges.remove(n.getEmoji());
                if (removed != null) reactionRow.getChildren().remove(removed);
            } else {
                renderBadge(n.getEmoji(), set);
            }
            refreshVisibility();
        }

        boolean userHasReaction(long userId, String emoji) {
            var set = reactions.get(emoji);
            return set != null && set.contains(userId);
        }

        private void renderBadge(String emoji, java.util.Set<Long> users) {
            Button badge = badges.get(emoji);
            String label = emoji + " " + users.size();
            if (badge == null) {
                badge = new Button(label);
                badge.getStyleClass().add("reaction-badge");
                badge.setOnAction(e -> toggleReaction(messageId, emoji));
                badges.put(emoji, badge);
                reactionRow.getChildren().add(badge);
            } else {
                badge.setText(label);
            }
            boolean mine = users.contains(currentUserId);
            badge.setStyle(mine
                    ? "-fx-background-color: #4f5d7a; -fx-text-fill: #ffffff; -fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 12px;"
                    : "-fx-background-color: #2f3136; -fx-text-fill: #dcddde; -fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 12px;");
        }

        private void refreshVisibility() {
            boolean any = !reactions.isEmpty();
            reactionRow.setVisible(any);
            reactionRow.setManaged(any);
        }
    }

    private static final class DmEntry {
        final long userId;
        final String displayName;
        DmEntry(long userId, String displayName) {
            this.userId = userId;
            this.displayName = displayName;
        }
        @Override public String toString() { return displayName; }
    }
}
