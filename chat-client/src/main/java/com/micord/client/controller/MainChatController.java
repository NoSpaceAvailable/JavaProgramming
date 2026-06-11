package com.micord.client.controller;

import com.micord.client.gif.GifPicker;
import com.micord.client.gif.GifResult;
import com.micord.client.gif.GiphyService;
import com.micord.client.net.FileTransferClient;
import com.micord.client.net.MessageListener;
import com.micord.client.net.ServerConnection;
import com.micord.client.util.SceneManager;
import com.micord.common.model.FileAttachment;
import com.micord.common.model.Message;
import com.micord.common.model.Reaction;
import com.micord.common.model.Room;
import com.micord.common.model.Server;
import com.micord.common.model.User;
import com.micord.common.model.UserStatus;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.notification.AddedToRoomNotification;
import com.micord.common.protocol.notification.ChannelCreatedNotification;
import com.micord.common.protocol.notification.MessageDeletedNotification;
import com.micord.common.protocol.notification.MessageEditedNotification;
import com.micord.common.protocol.notification.NewMessageNotification;
import com.micord.common.protocol.notification.ProfileUpdatedNotification;
import com.micord.common.protocol.notification.ReactionNotification;
import com.micord.common.protocol.notification.RemovedFromRoomNotification;
import com.micord.common.protocol.notification.StatusChangeNotification;
import com.micord.common.protocol.notification.UserJoinedNotification;
import com.micord.common.protocol.notification.UserTypingNotification;
import com.micord.common.protocol.notification.UserLeftNotification;
import com.micord.common.protocol.request.*;
import com.micord.common.protocol.response.*;
import com.micord.common.util.JsonUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MainChatController implements MessageListener {

    @FXML private Label userBadge;
    @FXML private Label connectionLabel;
    @FXML private Label conversationTitle;
    @FXML private Label membersTitle;
    @FXML private Button leaveButton;
    @FXML private Button inviteButton;
    @FXML private Button searchButton;
    @FXML private ListView<Room> roomListView;
    @FXML private ListView<Server> serverListView;
    @FXML private Label roomsHeader;
    @FXML private ListView<DmEntry> dmListView;
    @FXML private ListView<User> memberListView;
    @FXML private VBox messagesBox;
    @FXML private VBox membersPanel;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextField messageInput;
    @FXML private Button attachButton;
    @FXML private Button gifButton;
    @FXML private Label typingLabel;
    @FXML private Label uploadStatusLabel;
    @FXML private ComboBox<UserStatus> statusCombo;
    @FXML private VBox dropOverlay;
    @FXML private StackPane avatarPane;
    @FXML private Label avatarInitial;
    @FXML private StackPane headerAvatarPane;
    @FXML private Label headerAvatarInitial;
    @FXML private Label headerStatusLabel;

    private final ServerConnection connection = ServerConnection.getInstance();
    private static final String GIPHY_API_KEY = "PDHk52ymG4k6romXp7nSfhZlkJwA8Nwo";
    private final GiphyService giphyService = new GiphyService(GIPHY_API_KEY);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private long currentUserId;
    private String currentDisplayName;

    private final ObservableList<Room> rooms = FXCollections.observableArrayList();
    private final ObservableList<Server> servers = FXCollections.observableArrayList();
    private Long currentServerId; // null = Home (standalone rooms); otherwise the open server's id
    private final ObservableList<DmEntry> dms = FXCollections.observableArrayList();
    private final ObservableList<User> members = FXCollections.observableArrayList();
    // Cache of every user we've seen, used to render DM display names.
    private final Map<Long, User> knownUsers = new LinkedHashMap<>();
    // messageId -> rendered bubble container (so we can update reaction badges in place).
    private final Map<Long, MessageBubble> bubbles = new LinkedHashMap<>();
    // Unread counters keyed by conversation id (roomId or peer userId).
    private final Map<Long, Integer> roomUnread = new HashMap<>();
    private final Map<Long, Integer> dmUnread = new HashMap<>();
    private final Map<Long, List<StackPane>> messageAvatarNodes = new HashMap<>();
    // Tracked status per known user, kept in sync with STATUS_CHANGE_NOTIFICATION.
    private final Map<Long, UserStatus> userStatuses = new HashMap<>();
    private final Map<Long, String> activeTypingUsers = new LinkedHashMap<>();
    private final Map<Long, PauseTransition> typingExpiryTimers = new HashMap<>();
    private final PauseTransition localTypingIdleTimer = new PauseTransition(Duration.seconds(1.4));
    private boolean suppressStatusEvents;
    private boolean localTyping;

    private static final List<String> EMOJI_PALETTE = List.of(
            "👍", "❤️", "😂", "🎉", "😀",
            "😮", "😢", "🔥", "👀", "💩");

    private static final Map<String, String> EMOJI_IMAGES = Map.ofEntries(
            Map.entry("👍", "/emojis/thumbs_up_3d_default.png"),
            Map.entry("❤️", "/emojis/red_heart_3d.png"),
            Map.entry("😂", "/emojis/face_with_tears_of_joy_3d.png"),
            Map.entry("🎉", "/emojis/party_popper_3d.png"),
            Map.entry("😀", "/emojis/grinning_face_3d.png"),
            Map.entry("😮", "/emojis/shaking_face_3d.png"),
            Map.entry("😢", "/emojis/crying_face_3d.png"),
            Map.entry("🔥", "/emojis/fire_3d.png"),
            Map.entry("👀", "/emojis/eyes_3d.png"),
            Map.entry("💩", "/emojis/pile_of_poo_3d.png")
    );
    private static final Map<String, Image> EMOJI_IMAGE_CACHE = new HashMap<>();

    private static ImageView emojiImageView(String emoji, double size) {
        String path = EMOJI_IMAGES.get(emoji);
        if (path == null) return null;
        Image img = EMOJI_IMAGE_CACHE.computeIfAbsent(emoji, k ->
                new Image(MainChatController.class.getResourceAsStream(path)));
        ImageView iv = new ImageView(img);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        return iv;
    }

    private Conversation active;
    private Room currentRoom; // the Room object for the currently open room (null for DMs), used for owner checks
    private javafx.stage.Popup emojiPopup;
    private Image cachedAvatar;
    private final Map<Long, Image> userAvatarCache = new HashMap<>();
    private final java.util.Set<Long> avatarRequested = new java.util.HashSet<>();

    private final Runnable disconnectHandler = this::onServerDisconnected;

    @FXML
    public void initialize() {
        connection.addListener(this);
        connection.addDisconnectListener(disconnectHandler);
        FileTransferClient.getInstance().ensureRegistered();

        roomListView.setItems(rooms);
        roomListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                int unread = roomUnread.getOrDefault(item.getId(), 0);
                setText(unread > 0
                        ? "# " + item.getName() + "  (" + unread + ")"
                        : "# " + item.getName());
                setStyle(unread > 0 ? "-fx-font-weight: bold; -fx-text-fill: #ffffff;" : "");
            }
        });
        roomListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                dmListView.getSelectionModel().clearSelection();
                openRoom(newV);
            }
        });

        serverListView.setItems(servers);
        serverListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Server item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                    return;
                }
                setText("🏠 " + item.getName());
                setStyle("-fx-text-fill: #dcddde;");
                ContextMenu menu = new ContextMenu();
                MenuItem code = new MenuItem("Show invite code");
                code.setOnAction(e -> showInviteCode(item));
                menu.getItems().add(code);
                setContextMenu(menu);
            }
        });
        serverListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) onSelectServer(newV);
        });

        dmListView.setItems(dms);
        dmListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DmEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                    return;
                }
                UserStatus st = userStatuses.get(item.userId);
                StackPane avatar = createAvatarNode(item.userId, item.displayName, 28);
                StackPane avatarWithStatus = new StackPane(avatar);
                avatarWithStatus.setPrefSize(32, 32);
                Circle dot = statusCircle(st, 5);
                StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);
                avatarWithStatus.getChildren().add(dot);

                int unread = dmUnread.getOrDefault(item.userId, 0);
                String name = "@" + item.displayName;
                if (unread > 0) name += "  (" + unread + ")";
                Label nameLabel = new Label(name);
                nameLabel.setStyle(unread > 0
                        ? "-fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-font-size: 13px;"
                        : "-fx-text-fill: #dcddde; -fx-font-size: 13px;");
                HBox cell = new HBox(8, avatarWithStatus, nameLabel);
                cell.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(cell);
                setStyle("");
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
                    setGraphic(null);
                    return;
                }
                UserStatus liveStatus = userStatuses.getOrDefault(user.getId(), user.getStatus());
                StackPane avatar = createAvatarNode(user.getId(), user.getDisplayName(), 28);
                StackPane avatarWithStatus = new StackPane(avatar);
                avatarWithStatus.setPrefSize(32, 32);
                Circle dot = statusCircle(liveStatus, 5);
                StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);
                avatarWithStatus.getChildren().add(dot);

                Label nameLabel = new Label(user.getDisplayName());
                nameLabel.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 13px;");
                Label statusLabel = new Label(statusText(liveStatus));
                statusLabel.setStyle("-fx-text-fill: #72767d; -fx-font-size: 11px;");
                VBox info = new VBox(0, nameLabel, statusLabel);
                HBox cell = new HBox(8, avatarWithStatus, info);
                cell.setAlignment(Pos.CENTER_LEFT);
                setText(null);
                setGraphic(cell);

                // Owner-only: right-click a member to remove them from the room.
                boolean canKick = currentRoom != null
                        && currentRoom.getOwnerId() == currentUserId
                        && user.getId() != currentUserId;
                if (canKick) {
                    ContextMenu menu = new ContextMenu();
                    MenuItem kick = new MenuItem("Remove from room");
                    long targetId = user.getId();
                    String targetName = user.getDisplayName();
                    kick.setOnAction(ev -> confirmRemoveMember(targetId, targetName));
                    menu.getItems().add(kick);
                    setContextMenu(menu);
                } else {
                    setContextMenu(null);
                }
            }
        });
        memberListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                User u = memberListView.getSelectionModel().getSelectedItem();
                if (u != null && u.getId() != currentUserId) startDm(u);
            }
        });

        statusCombo.getItems().setAll(UserStatus.ONLINE, UserStatus.AWAY);
        statusCombo.setValue(UserStatus.ONLINE);
        statusCombo.valueProperty().addListener((obs, oldV, newV) -> {
            if (suppressStatusEvents || newV == null) return;
            connection.send(JsonUtil.wrap(MessageType.STATUS_UPDATE_REQUEST,
                    new StatusUpdateRequest(newV)));
        });

        messagesScrollPane.setOnDragOver(this::onDragOver);
        messagesScrollPane.setOnDragDropped(this::onDragDropped);
        messagesScrollPane.setOnDragExited(e -> showDropOverlay(false));

        avatarPane.setOnMouseClicked(e -> showProfileDialog());
        userBadge.setOnMouseClicked(e -> showProfileDialog());

        localTypingIdleTimer.setOnFinished(e -> sendTypingState(false));
        messageInput.textProperty().addListener((obs, oldV, newV) -> {
            if (active == null || messageInput.isDisabled()) return;
            if (newV != null && !newV.isBlank()) {
                sendTypingState(true);
                localTypingIdleTimer.playFromStart();
            } else {
                localTypingIdleTimer.stop();
                sendTypingState(false);
            }
        });
    }

    private void onDragOver(DragEvent event) {
        if (active != null && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
            showDropOverlay(true);
        }
        event.consume();
    }

    private void onDragDropped(DragEvent event) {
        showDropOverlay(false);
        Dragboard db = event.getDragboard();
        if (active != null && db.hasFiles()) {
            for (File file : db.getFiles()) {
                uploadFile(file);
            }
            event.setDropCompleted(true);
        } else {
            event.setDropCompleted(false);
        }
        event.consume();
    }

    private void showDropOverlay(boolean show) {
        dropOverlay.setVisible(show);
        dropOverlay.setManaged(show);
    }

    private static String statusDot(UserStatus status) {
        if (status == null) return "○";
        return switch (status) {
            case ONLINE -> "●";
            case AWAY -> "◐";
            case OFFLINE -> "○";
        };
    }

    private static Circle statusCircle(UserStatus status, double radius) {
        Color color = switch (status != null ? status : UserStatus.OFFLINE) {
            case ONLINE -> Color.web("#43b581");
            case AWAY -> Color.web("#faa61a");
            case OFFLINE -> Color.web("#747f8d");
        };
        Circle c = new Circle(radius, color);
        c.setStroke(Color.web("#2f3136"));
        c.setStrokeWidth(1.5);
        return c;
    }

    private static String statusText(UserStatus status) {
        if (status == null) return "Offline";
        return switch (status) {
            case ONLINE -> "Online";
            case AWAY -> "Away";
            case OFFLINE -> "Offline";
        };
    }

    private void requestAvatarIfNeeded(long userId) {
        if (userAvatarCache.containsKey(userId) || !avatarRequested.add(userId)) return;
        connection.send(JsonUtil.wrap(MessageType.AVATAR_REQUEST,
                new com.micord.common.protocol.request.AvatarRequest(userId)));
    }

    private StackPane createAvatarNode(long userId, String displayName, double size) {
        StackPane pane = new StackPane();
        pane.setPrefSize(size, size);
        pane.setMinSize(size, size);
        pane.setMaxSize(size, size);
        double radius = size / 2;
        pane.setStyle("-fx-background-color: #7289da; -fx-background-radius: " + radius + ";");

        Image avatar = userAvatarCache.get(userId);
        if (avatar != null) {
            applyAvatarToPane(pane, avatar, size);
        } else {
            String initial = (displayName != null && !displayName.isEmpty())
                    ? String.valueOf(Character.toUpperCase(displayName.charAt(0))) : "?";
            Label lbl = new Label(initial);
            lbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: " + (size * 0.45) + "px; -fx-font-weight: bold;");
            pane.getChildren().add(lbl);
            requestAvatarIfNeeded(userId);
        }
        return pane;
    }

    public void initWithUser(long userId, String displayName, List<Room> initialRooms, List<User> recentDmPeers) {
        this.currentUserId = userId;
        this.currentDisplayName = displayName;
        userBadge.setText("@" + displayName);
        updateAvatarInitial(displayName);
        if (initialRooms != null) rooms.setAll(initialRooms);
        if (recentDmPeers != null) {
            for (User peer : recentDmPeers) {
                dms.add(new DmEntry(peer.getId(), peer.getDisplayName()));
                knownUsers.put(peer.getId(), peer);
                if (peer.getStatus() != null) userStatuses.put(peer.getId(), peer.getStatus());
            }
        }
        attachButton.setDisable(true);
        gifButton.setDisable(true);
        showSystemPlaceholder("Select a room from the left or start a DM.");
        connection.send(JsonUtil.wrap(MessageType.LIST_USERS_REQUEST, new Object()));
        connection.send(JsonUtil.wrap(MessageType.LIST_SERVERS_REQUEST, new Object()));
        loadOwnAvatar();
    }

    private void updateAvatarInitial(String name) {
        avatarInitial.setText(name != null && !name.isEmpty()
                ? String.valueOf(Character.toUpperCase(name.charAt(0))) : "?");
    }

    private void loadOwnAvatar() {
        connection.send(JsonUtil.wrap(MessageType.AVATAR_REQUEST,
                new com.micord.common.protocol.request.AvatarRequest(currentUserId)));
    }

    // ---- UI handlers ----

    @FXML
    private void handleCreateRoom() {
        // The "+" next to the room list is context-aware: inside a server it creates a channel.
        if (currentServerId != null) {
            handleCreateChannel();
            return;
        }
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

    // ---- Servers + channels ----

    @FXML
    private void handleCreateServer() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Create Server");
        d.setHeaderText("Create a community server");
        d.setContentText("Server name:");
        d.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(name ->
                connection.send(JsonUtil.wrap(MessageType.CREATE_SERVER_REQUEST, new CreateServerRequest(name))));
    }

    @FXML
    private void handleJoinServer() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Join Server");
        d.setHeaderText("Join a server with an invite code");
        d.setContentText("Invite code:");
        d.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(code ->
                connection.send(JsonUtil.wrap(MessageType.JOIN_SERVER_REQUEST, new JoinServerRequest(code))));
    }

    @FXML
    private void handleHome() {
        currentServerId = null;
        serverListView.getSelectionModel().clearSelection();
        roomsHeader.setText("ROOMS");
        connection.send(JsonUtil.wrap(MessageType.LIST_ROOMS_REQUEST, new Object()));
    }

    private void handleCreateChannel() {
        Long serverId = currentServerId;
        if (serverId == null) return;
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Create Channel");
        d.setHeaderText("New text channel");
        d.setContentText("Channel name:");
        d.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(name ->
                connection.send(JsonUtil.wrap(MessageType.CREATE_CHANNEL_REQUEST,
                        new CreateChannelRequest(serverId, name))));
    }

    private void onSelectServer(Server server) {
        currentServerId = server.getId();
        roomsHeader.setText("📋 " + server.getName().toUpperCase());
        connection.send(JsonUtil.wrap(MessageType.LIST_CHANNELS_REQUEST,
                new ListChannelsRequest(server.getId())));
    }

    private void showInviteCode(Server server) {
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(server.getInviteCode());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        showAlert(Alert.AlertType.INFORMATION,
                "Invite code for \"" + server.getName() + "\":  " + server.getInviteCode()
                        + "\n(copied to clipboard)");
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
    private void handleInviteMember() {
        if (active == null || active.kind != Conversation.Kind.ROOM) return;
        long roomId = active.id;

        // Candidates = known users not already in this room.
        java.util.Set<Long> memberIds = new java.util.HashSet<>();
        for (User m : members) memberIds.add(m.getId());
        Map<String, User> labelToUser = new LinkedHashMap<>();
        for (User u : knownUsers.values()) {
            if (u.getId() == currentUserId || memberIds.contains(u.getId())) continue;
            String dot = switch (u.getStatus() != null ? u.getStatus() : UserStatus.OFFLINE) {
                case ONLINE -> "● ";
                case AWAY -> "◐ ";
                case OFFLINE -> "○ ";
            };
            labelToUser.put(dot + u.getDisplayName() + " (@" + u.getUsername() + ")", u);
        }
        if (labelToUser.isEmpty()) {
            // Refresh the user list in case it is stale, then inform the user.
            connection.send(JsonUtil.wrap(MessageType.LIST_USERS_REQUEST, new Object()));
            showAlert(Alert.AlertType.INFORMATION, "No other users available to add right now.");
            return;
        }
        List<String> labels = new ArrayList<>(labelToUser.keySet());
        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle("Add member");
        dialog.setHeaderText("Add a user to \"" + active.title + "\"");
        dialog.setContentText("User:");
        dialog.showAndWait().map(labelToUser::get).ifPresent(u ->
                connection.send(JsonUtil.wrap(MessageType.INVITE_TO_ROOM_REQUEST,
                        new InviteToRoomRequest(roomId, u.getId()))));
    }

    @FXML
    private void handleSearchMessages() {
        if (active == null) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search messages");
        dialog.setHeaderText("Search in " + (active.kind == Conversation.Kind.ROOM ? "#" : "@") + active.title);
        dialog.setContentText("Find:");
        dialog.showAndWait().map(String::trim).filter(q -> !q.isEmpty()).ifPresent(query -> {
            SearchMessagesRequest req = active.kind == Conversation.Kind.ROOM
                    ? new SearchMessagesRequest(active.id, null, query)
                    : new SearchMessagesRequest(null, active.id, query);
            connection.send(JsonUtil.wrap(MessageType.SEARCH_MESSAGES_REQUEST, req));
        });
    }

    private void confirmRemoveMember(long userId, String displayName) {
        if (active == null || active.kind != Conversation.Kind.ROOM) return;
        long roomId = active.id;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + displayName + " from \"" + active.title + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b ->
                connection.send(JsonUtil.wrap(MessageType.REMOVE_MEMBER_REQUEST,
                        new RemoveMemberRequest(roomId, userId))));
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
        sendTypingState(false);
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
    private void handleGifButton() {
        if (active == null) return;
        GifPicker picker = new GifPicker(giphyService);
        picker.show(SceneManager.getPrimaryStage(), gif -> sendGifMessage(gif));
    }

    private void sendGifMessage(GifResult gif) {
        if (active == null) return;
        if (active.kind == Conversation.Kind.ROOM) {
            connection.send(JsonUtil.wrap(MessageType.SEND_MESSAGE_REQUEST,
                    new SendMessageRequest(active.id, gif.gifUrl(), "GIF")));
        } else {
            connection.send(JsonUtil.wrap(MessageType.PRIVATE_MESSAGE_REQUEST,
                    new PrivateMessageRequest(active.id, gif.gifUrl(), "GIF")));
        }
        giphyService.registerOnSent(gif);
    }

    @FXML
    private void handleAttachFile() {
        if (active == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Send file");
        File file = chooser.showOpenDialog(SceneManager.getPrimaryStage());
        if (file != null) uploadFile(file);
    }

    private void uploadFile(File file) {
        if (active == null) return;
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
        gifButton.setDisable(true);
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
                        if (active != null) { attachButton.setDisable(false); gifButton.setDisable(false); }
                    }
                    @Override
                    public void onError(String msg) {
                        hideUploadStatus();
                        if (active != null) { attachButton.setDisable(false); gifButton.setDisable(false); }
                        showAlert(Alert.AlertType.ERROR, "Upload failed: " + msg);
                    }
                });
    }

    @FXML
    private void handleLogout() {
        sendTypingState(false);
        connection.removeListener(this);
        connection.removeDisconnectListener(disconnectHandler);
        connection.disconnect();
        LoginController.disableAutoLogin();
        SceneManager.switchTo("login");
    }

    private void onServerDisconnected() {
        connectionLabel.setText("Disconnected");
        connectionLabel.setStyle("-fx-text-fill: #f04747; -fx-font-size: 12px;");
        messageInput.setDisable(true);
        attachButton.setDisable(true);
        gifButton.setDisable(true);
        statusCombo.setDisable(true);
    }

    // ---- Conversation switching ----

    private void openRoom(Room room) {
        sendTypingState(false);
        active = new Conversation(Conversation.Kind.ROOM, room.getId(), room.getName());
        currentRoom = room;
        if (roomUnread.remove(room.getId()) != null) roomListView.refresh();
        conversationTitle.setText("# " + room.getName());
        leaveButton.setVisible(true);
        leaveButton.setManaged(true);
        inviteButton.setVisible(true);
        inviteButton.setManaged(true);
        searchButton.setVisible(true);
        searchButton.setManaged(true);
        membersPanel.setVisible(true);
        membersPanel.setManaged(true);
        membersTitle.setText("MEMBERS");
        members.clear();
        messageInput.setDisable(false);
        attachButton.setDisable(false);
        gifButton.setDisable(false);
        messageInput.setPromptText("Message #" + room.getName());
        headerAvatarPane.setVisible(false);
        headerAvatarPane.setManaged(false);
        headerStatusLabel.setVisible(false);
        headerStatusLabel.setManaged(false);
        clearMessages();
        clearTypingIndicator();
        connection.send(JsonUtil.wrap(MessageType.GET_HISTORY_REQUEST,
                new GetHistoryRequest(room.getId(), 0, 50)));
        connection.send(JsonUtil.wrap(MessageType.ROOM_MEMBERS_REQUEST,
                new RoomMembersRequest(room.getId())));
    }

    private void openDm(DmEntry dm) {
        sendTypingState(false);
        active = new Conversation(Conversation.Kind.DM, dm.userId, dm.displayName);
        currentRoom = null;
        if (dmUnread.remove(dm.userId) != null) dmListView.refresh();
        conversationTitle.setText("@ " + dm.displayName);
        leaveButton.setVisible(false);
        leaveButton.setManaged(false);
        inviteButton.setVisible(false);
        inviteButton.setManaged(false);
        searchButton.setVisible(true);
        searchButton.setManaged(true);
        membersPanel.setVisible(false);
        membersPanel.setManaged(false);
        messageInput.setDisable(false);
        attachButton.setDisable(false);
        gifButton.setDisable(false);
        messageInput.setPromptText("Message @" + dm.displayName);
        updateDmHeader(dm.userId, dm.displayName);
        clearMessages();
        clearTypingIndicator();
        connection.send(JsonUtil.wrap(MessageType.DM_HISTORY_REQUEST,
                new DmHistoryRequest(dm.userId, 0, 50)));
    }

    private void updateDmHeader(long peerId, String displayName) {
        headerAvatarPane.setVisible(true);
        headerAvatarPane.setManaged(true);
        Image peerAvatar = userAvatarCache.get(peerId);
        if (peerAvatar != null) {
            applyAvatarToPane(headerAvatarPane, peerAvatar, 28);
        } else {
            String initial = (displayName != null && !displayName.isEmpty())
                    ? String.valueOf(Character.toUpperCase(displayName.charAt(0))) : "?";
            headerAvatarInitial.setText(initial);
            headerAvatarPane.getChildren().setAll(headerAvatarInitial);
            requestAvatarIfNeeded(peerId);
        }
        UserStatus st = userStatuses.get(peerId);
        headerStatusLabel.setText(statusText(st));
        Color stColor = switch (st != null ? st : UserStatus.OFFLINE) {
            case ONLINE -> Color.web("#43b581");
            case AWAY -> Color.web("#faa61a");
            case OFFLINE -> Color.web("#747f8d");
        };
        headerStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + toHex(stColor) + ";");
        headerStatusLabel.setVisible(true);
        headerStatusLabel.setManaged(true);
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }

    private void clearMessages() {
        messagesBox.getChildren().clear();
        bubbles.clear();
        messageAvatarNodes.clear();
        if (typingLabel != null) {
            typingLabel.setVisible(false);
            typingLabel.setManaged(false);
        }
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
            case INVITE_TO_ROOM_RESPONSE -> onInviteResponse(message);
            case ADDED_TO_ROOM_NOTIFICATION -> onAddedToRoom(message);
            case REMOVE_MEMBER_RESPONSE -> onRemoveMemberResponse(message);
            case REMOVED_FROM_ROOM_NOTIFICATION -> onRemovedFromRoom(message);
            case LIST_ROOMS_RESPONSE -> onRoomListResponse(message, false);
            case LIST_PUBLIC_ROOMS_RESPONSE -> onRoomListResponse(message, true);
            case ROOM_MEMBERS_RESPONSE -> onRoomMembersResponse(message);
            case LIST_USERS_RESPONSE -> onUserListResponse(message);
            case CREATE_SERVER_RESPONSE -> onCreateServerResponse(message);
            case LIST_SERVERS_RESPONSE -> onServerListResponse(message);
            case JOIN_SERVER_RESPONSE -> onJoinServerResponse(message);
            case CREATE_CHANNEL_RESPONSE -> onCreateChannelResponse(message);
            case LIST_CHANNELS_RESPONSE -> onChannelListResponse(message);
            case CHANNEL_CREATED_NOTIFICATION -> onChannelCreated(message);
            case GET_HISTORY_RESPONSE -> onHistoryResponse(message);
            case SEARCH_MESSAGES_RESPONSE -> onSearchResponse(message);
            case MESSAGE_EDITED_NOTIFICATION -> onMessageEdited(message);
            case MESSAGE_DELETED_NOTIFICATION -> onMessageDeleted(message);
            case EDIT_MESSAGE_RESPONSE -> {
                EditMessageResponse r = JsonUtil.fromJson(message.getPayload(), EditMessageResponse.class);
                if (!r.isSuccess()) showAlert(Alert.AlertType.ERROR, r.getMessage());
            }
            case DELETE_MESSAGE_RESPONSE -> {
                DeleteMessageResponse r = JsonUtil.fromJson(message.getPayload(), DeleteMessageResponse.class);
                if (!r.isSuccess()) showAlert(Alert.AlertType.ERROR, r.getMessage());
            }
            case NEW_MESSAGE_NOTIFICATION -> onNewMessage(message);
            case REACTION_NOTIFICATION -> onReactionNotification(message);
            case STATUS_CHANGE_NOTIFICATION -> onStatusChange(message);
            case USER_TYPING_NOTIFICATION -> onTypingNotification(message);
            case PROFILE_UPDATED_NOTIFICATION -> onProfileUpdated(message);
            case STATUS_UPDATE_RESPONSE -> onStatusUpdateResponse(message);
            case USER_JOINED_NOTIFICATION -> onUserJoined(message);
            case USER_LEFT_NOTIFICATION -> onUserLeft(message);
            case UPDATE_PROFILE_RESPONSE -> onUpdateProfileResponse(message);
            case AVATAR_RESPONSE -> onAvatarResponse(message);
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
            currentRoom = null;
            conversationTitle.setText("Select a room or DM");
            leaveButton.setVisible(false);
            leaveButton.setManaged(false);
            inviteButton.setVisible(false);
            inviteButton.setManaged(false);
            searchButton.setVisible(false);
            searchButton.setManaged(false);
            clearMessages();
            clearTypingIndicator();
            messageInput.setDisable(true);
            attachButton.setDisable(true);
            gifButton.setDisable(true);
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
            for (User u : r.getMembers()) {
                knownUsers.put(u.getId(), u);
                if (u.getStatus() != null) userStatuses.put(u.getId(), u.getStatus());
            }
            membersTitle.setText("MEMBERS — " + r.getMembers().size());
        }
    }

    private void onInviteResponse(ProtocolMessage m) {
        InviteToRoomResponse r = JsonUtil.fromJson(m.getPayload(), InviteToRoomResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
            return;
        }
        // The member panel refreshes via USER_JOINED_NOTIFICATION; just confirm.
        showAlert(Alert.AlertType.INFORMATION, r.getMessage());
    }

    private void onAddedToRoom(ProtocolMessage m) {
        AddedToRoomNotification n = JsonUtil.fromJson(m.getPayload(), AddedToRoomNotification.class);
        Room room = n.getRoom();
        if (room == null) return;
        if (rooms.stream().noneMatch(rr -> rr.getId() == room.getId())) {
            rooms.add(room);
        }
        showAlert(Alert.AlertType.INFORMATION,
                (n.getInviterName() != null ? n.getInviterName() : "Someone")
                        + " added you to \"" + room.getName() + "\".");
    }

    private void onRemoveMemberResponse(ProtocolMessage m) {
        RemoveMemberResponse r = JsonUtil.fromJson(m.getPayload(), RemoveMemberResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
        }
        // On success the member panel refreshes via USER_LEFT_NOTIFICATION.
    }

    private void onRemovedFromRoom(ProtocolMessage m) {
        RemovedFromRoomNotification n = JsonUtil.fromJson(m.getPayload(), RemovedFromRoomNotification.class);
        rooms.removeIf(rr -> rr.getId() == n.getRoomId());
        if (active != null && active.kind == Conversation.Kind.ROOM && active.id == n.getRoomId()) {
            active = null;
            currentRoom = null;
            conversationTitle.setText("Select a room or DM");
            leaveButton.setVisible(false);
            leaveButton.setManaged(false);
            inviteButton.setVisible(false);
            inviteButton.setManaged(false);
            searchButton.setVisible(false);
            searchButton.setManaged(false);
            membersPanel.setVisible(false);
            membersPanel.setManaged(false);
            clearMessages();
            members.clear();
            messageInput.setDisable(true);
            attachButton.setDisable(true);
            gifButton.setDisable(true);
        }
        showAlert(Alert.AlertType.INFORMATION,
                (n.getRemoverName() != null ? n.getRemoverName() : "The owner")
                        + " removed you from \"" + n.getRoomName() + "\".");
    }

    private void onMessageEdited(ProtocolMessage m) {
        MessageEditedNotification n = JsonUtil.fromJson(m.getPayload(), MessageEditedNotification.class);
        MessageBubble bubble = bubbles.get(n.getMessageId());
        if (bubble != null) bubble.applyEdit(n.getContent());
    }

    private void onMessageDeleted(ProtocolMessage m) {
        MessageDeletedNotification n = JsonUtil.fromJson(m.getPayload(), MessageDeletedNotification.class);
        MessageBubble bubble = bubbles.remove(n.getMessageId());
        if (bubble != null) messagesBox.getChildren().remove(bubble.root);
    }

    private void onSearchResponse(ProtocolMessage m) {
        SearchMessagesResponse r = JsonUtil.fromJson(m.getPayload(), SearchMessagesResponse.class);
        List<Message> results = r.getResults() == null ? List.of() : r.getResults();
        if (results.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No messages found for \"" + r.getQuery() + "\".");
            return;
        }

        ListView<Message> list = new ListView<>();
        list.getItems().setAll(results);
        list.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Message msg, boolean empty) {
                super.updateItem(msg, empty);
                if (empty || msg == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                String when = msg.getCreatedAt() != null
                        ? msg.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm"))
                        : "";
                Label header = new Label(msg.getSenderName() + "  •  " + when);
                header.setStyle("-fx-text-fill: #7289da; -fx-font-size: 11px; -fx-font-weight: bold;");
                String body = msg.getContent() == null ? "" : msg.getContent();
                if (msg.getMessageType() != null && msg.getMessageType() != Message.MessageType.TEXT) {
                    body = "[" + msg.getMessageType() + "] " + body;
                }
                Label content = new Label(body);
                content.setWrapText(true);
                content.setStyle("-fx-text-fill: #dcddde; -fx-font-size: 13px;");
                VBox box = new VBox(2, header, content);
                box.setStyle("-fx-padding: 6 4 6 4;");
                setGraphic(box);
                setText(null);
            }
        });

        Label title = new Label(results.size() + " result(s) for \"" + r.getQuery() + "\"");
        title.setStyle("-fx-text-fill: #b9bbbe; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 8;");
        VBox root = new VBox(title, list);
        VBox.setVgrow(list, javafx.scene.layout.Priority.ALWAYS);
        root.setStyle("-fx-background-color: #36393f;");

        javafx.scene.Scene scene = new javafx.scene.Scene(root, 460, 520);
        var css = getClass().getResource("/css/dark-theme.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("Search results");
        stage.initModality(javafx.stage.Modality.NONE);
        stage.setScene(scene);
        stage.show();
    }

    private void onCreateServerResponse(ProtocolMessage m) {
        CreateServerResponse r = JsonUtil.fromJson(m.getPayload(), CreateServerResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
            return;
        }
        Server s = r.getServer();
        if (servers.stream().noneMatch(x -> x.getId() == s.getId())) servers.add(s);
        serverListView.getSelectionModel().select(s); // triggers onSelectServer -> loads channels
        showAlert(Alert.AlertType.INFORMATION,
                "Server \"" + s.getName() + "\" created!\nInvite code: " + s.getInviteCode()
                        + "\n(Share it so others can join. Right-click the server to see it again.)");
    }

    private void onServerListResponse(ProtocolMessage m) {
        ServerListResponse r = JsonUtil.fromJson(m.getPayload(), ServerListResponse.class);
        servers.setAll(r.getServers() == null ? List.of() : r.getServers());
    }

    private void onJoinServerResponse(ProtocolMessage m) {
        JoinServerResponse r = JsonUtil.fromJson(m.getPayload(), JoinServerResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
            return;
        }
        Server s = r.getServer();
        if (servers.stream().noneMatch(x -> x.getId() == s.getId())) servers.add(s);
        serverListView.getSelectionModel().select(s);
        showAlert(Alert.AlertType.INFORMATION, r.getMessage());
    }

    private void onCreateChannelResponse(ProtocolMessage m) {
        CreateChannelResponse r = JsonUtil.fromJson(m.getPayload(), CreateChannelResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
            return;
        }
        Room ch = r.getChannel();
        if (ch != null && currentServerId != null && ch.getServerId() != null
                && ch.getServerId().longValue() == currentServerId.longValue()) {
            if (rooms.stream().noneMatch(x -> x.getId() == ch.getId())) rooms.add(ch);
            roomListView.getSelectionModel().select(ch);
        }
    }

    private void onChannelListResponse(ProtocolMessage m) {
        ChannelListResponse r = JsonUtil.fromJson(m.getPayload(), ChannelListResponse.class);
        if (currentServerId == null || r.getServerId() != currentServerId) return; // stale response
        rooms.setAll(r.getChannels() == null ? List.of() : r.getChannels());
    }

    private void onChannelCreated(ProtocolMessage m) {
        ChannelCreatedNotification n = JsonUtil.fromJson(m.getPayload(), ChannelCreatedNotification.class);
        if (currentServerId != null && n.getServerId() == currentServerId && n.getChannel() != null
                && rooms.stream().noneMatch(x -> x.getId() == n.getChannel().getId())) {
            rooms.add(n.getChannel());
        }
    }

    private void onUserListResponse(ProtocolMessage m) {
        UserListResponse r = JsonUtil.fromJson(m.getPayload(), UserListResponse.class);
        if (r.getUsers() != null) {
            knownUsers.clear();
            for (User u : r.getUsers()) {
                knownUsers.put(u.getId(), u);
                if (u.getStatus() != null) userStatuses.put(u.getId(), u.getStatus());
            }
            dmListView.refresh();
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
        if (matchesActiveConversation(n)) {
            removeTypingUser(n.getSenderId());
        }
        boolean isRoom = n.getRoomId() != null && n.getRoomId() > 0;
        if (isRoom) {
            boolean isActive = active != null && active.kind == Conversation.Kind.ROOM
                    && active.id == n.getRoomId();
            if (isActive) {
                appendAndScroll(n);
            } else if (n.getSenderId() != currentUserId) {
                roomUnread.merge(n.getRoomId(), 1, Integer::sum);
                roomListView.refresh();
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
        boolean isActiveDm = active != null && active.kind == Conversation.Kind.DM && active.id == peerId;
        if (isActiveDm) {
            appendAndScroll(n);
        } else if (n.getSenderId() != currentUserId) {
            dmUnread.merge(peerId, 1, Integer::sum);
            dmListView.refresh();
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

    private void onStatusChange(ProtocolMessage m) {
        StatusChangeNotification n = JsonUtil.fromJson(m.getPayload(), StatusChangeNotification.class);
        userStatuses.put(n.getUserId(), n.getStatus());
        User cached = knownUsers.get(n.getUserId());
        if (cached != null) cached.setStatus(n.getStatus());
        dmListView.refresh();
        memberListView.refresh();
        if (active != null && active.kind == Conversation.Kind.DM && active.id == n.getUserId()) {
            updateDmHeader(n.getUserId(), active.title);
        }
    }

    private void onProfileUpdated(ProtocolMessage m) {
        ProfileUpdatedNotification n = JsonUtil.fromJson(m.getPayload(), ProfileUpdatedNotification.class);
        User cached = knownUsers.get(n.getUserId());
        if (cached != null) {
            cached.setDisplayName(n.getDisplayName());
        } else {
            knownUsers.put(n.getUserId(), new User(n.getUserId(), "", n.getDisplayName()));
        }

        for (User member : members) {
            if (member.getId() == n.getUserId()) {
                member.setDisplayName(n.getDisplayName());
                break;
            }
        }

        DmEntry dm = findDm(n.getUserId());
        if (dm != null) dm.displayName = n.getDisplayName();

        if (active != null && active.kind == Conversation.Kind.DM && active.id == n.getUserId()) {
            active = new Conversation(Conversation.Kind.DM, n.getUserId(), n.getDisplayName());
            conversationTitle.setText("@ " + n.getDisplayName());
            messageInput.setPromptText("Message @" + n.getDisplayName());
            updateDmHeader(n.getUserId(), n.getDisplayName());
        }

        if (n.isAvatarUpdated()) {
            userAvatarCache.remove(n.getUserId());
            avatarRequested.remove(n.getUserId());
            requestAvatarIfNeeded(n.getUserId());
        }

        dmListView.refresh();
        memberListView.refresh();
    }

    private void onStatusUpdateResponse(ProtocolMessage m) {
        StatusUpdateResponse r = JsonUtil.fromJson(m.getPayload(), StatusUpdateResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
            return;
        }
        userStatuses.put(currentUserId, r.getStatus());
    }

    private void onReactionNotification(ProtocolMessage m) {
        ReactionNotification n = JsonUtil.fromJson(m.getPayload(), ReactionNotification.class);
        MessageBubble bubble = bubbles.get(n.getMessageId());
        if (bubble == null) return;
        bubble.applyReaction(n);
    }

    private void onTypingNotification(ProtocolMessage m) {
        UserTypingNotification n = JsonUtil.fromJson(m.getPayload(), UserTypingNotification.class);
        if (n.getSenderId() == currentUserId || !matchesActiveConversation(n)) return;
        if (n.isTyping()) {
            activeTypingUsers.put(n.getSenderId(), n.getSenderName());
            PauseTransition timer = typingExpiryTimers.computeIfAbsent(n.getSenderId(),
                    ignored -> new PauseTransition(Duration.seconds(3)));
            timer.setOnFinished(e -> removeTypingUser(n.getSenderId()));
            timer.playFromStart();
        } else {
            removeTypingUser(n.getSenderId());
        }
        refreshTypingLabel();
    }

    private boolean matchesActiveConversation(UserTypingNotification n) {
        if (active == null) return false;
        if (active.kind == Conversation.Kind.ROOM) {
            return n.getRoomId() != null && n.getRoomId() == active.id;
        }
        return n.getRecipientId() != null && n.getRecipientId() == currentUserId && n.getSenderId() == active.id;
    }

    private boolean matchesActiveConversation(NewMessageNotification n) {
        if (active == null) return false;
        if (active.kind == Conversation.Kind.ROOM) {
            return n.getRoomId() != null && n.getRoomId() == active.id;
        }
        long peerId = n.getSenderId() == currentUserId
                ? (n.getRecipientId() != null ? n.getRecipientId() : 0)
                : n.getSenderId();
        return peerId == active.id;
    }

    private void sendTypingState(boolean typing) {
        if (active == null || localTyping == typing || !connection.isConnected()) return;
        localTyping = typing;
        Long roomId = active.kind == Conversation.Kind.ROOM ? active.id : null;
        Long recipientId = active.kind == Conversation.Kind.DM ? active.id : null;
        connection.send(JsonUtil.wrap(MessageType.TYPING_REQUEST,
                new TypingRequest(roomId, recipientId, typing)));
    }

    private void removeTypingUser(long userId) {
        activeTypingUsers.remove(userId);
        PauseTransition timer = typingExpiryTimers.remove(userId);
        if (timer != null) timer.stop();
        refreshTypingLabel();
    }

    private void clearTypingIndicator() {
        activeTypingUsers.clear();
        for (PauseTransition timer : typingExpiryTimers.values()) {
            timer.stop();
        }
        typingExpiryTimers.clear();
        refreshTypingLabel();
    }

    private void refreshTypingLabel() {
        if (activeTypingUsers.isEmpty()) {
            typingLabel.setVisible(false);
            typingLabel.setManaged(false);
            typingLabel.setText("");
            return;
        }
        Set<String> names = new java.util.LinkedHashSet<>(activeTypingUsers.values());
        String text;
        if (names.size() == 1) {
            text = names.iterator().next() + " is typing...";
        } else if (names.size() == 2) {
            text = String.join(" and ", names) + " are typing...";
        } else {
            text = names.iterator().next() + " and others are typing...";
        }
        typingLabel.setText(text);
        typingLabel.setVisible(true);
        typingLabel.setManaged(true);
    }

    private void onUpdateProfileResponse(ProtocolMessage m) {
        UpdateProfileResponse r = JsonUtil.fromJson(m.getPayload(), UpdateProfileResponse.class);
        if (!r.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, r.getMessage());
            return;
        }
        currentDisplayName = r.getDisplayName();
        userBadge.setText("@" + currentDisplayName);
        updateAvatarInitial(currentDisplayName);
        if (r.getAvatarUrl() != null) loadOwnAvatar();
        showAlert(Alert.AlertType.INFORMATION, "Profile updated!");
    }

    private void onAvatarResponse(ProtocolMessage m) {
        AvatarResponse r = JsonUtil.fromJson(m.getPayload(), AvatarResponse.class);
        if (r.getAvatarData() == null) {
            userAvatarCache.remove(r.getUserId());
            refreshMessageAvatarNodes(r.getUserId(), null);
            return;
        }
        try {
            byte[] data = java.util.Base64.getDecoder().decode(r.getAvatarData());
            Image img = new Image(new java.io.ByteArrayInputStream(data));
            userAvatarCache.put(r.getUserId(), img);
            if (r.getUserId() == currentUserId) {
                cachedAvatar = img;
                applyAvatarToPane(avatarPane, img, 32);
            }
            if (active != null && active.kind == Conversation.Kind.DM && active.id == r.getUserId()) {
                applyAvatarToPane(headerAvatarPane, img, 28);
            }
            refreshMessageAvatarNodes(r.getUserId(), img);
            memberListView.refresh();
            dmListView.refresh();
        } catch (Exception ignored) {}
    }

    private void applyAvatarToPane(StackPane pane, Image img, double size) {
        ImageView iv = new ImageView(img);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(size / 2, size / 2, size / 2);
        iv.setClip(clip);
        pane.getChildren().setAll(iv);
    }

    private void refreshMessageAvatarNodes(long userId, Image img) {
        List<StackPane> nodes = messageAvatarNodes.get(userId);
        if (nodes == null || img == null) return;
        for (StackPane pane : nodes) {
            applyAvatarToPane(pane, img, 36);
        }
    }

    private void onError(ProtocolMessage m) {
        ErrorResponse r = JsonUtil.fromJson(m.getPayload(), ErrorResponse.class);
        showAlert(Alert.AlertType.ERROR, r.getMessage());
    }

    // ---- Profile ----

    private void showProfileDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Profile");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #36393f;");
        pane.getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 20;");

        StackPane avatarPreview = new StackPane();
        avatarPreview.setPrefSize(80, 80);
        avatarPreview.setMinSize(80, 80);
        avatarPreview.setMaxSize(80, 80);
        avatarPreview.setStyle("-fx-background-color: #7289da; -fx-background-radius: 40; -fx-cursor: hand;");
        if (cachedAvatar != null) {
            applyAvatarToPane(avatarPreview, cachedAvatar, 80);
        } else {
            Label avatarLetter = new Label(avatarInitial.getText());
            avatarLetter.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 32px; -fx-font-weight: bold;");
            avatarPreview.getChildren().add(avatarLetter);
        }

        Label changeAvatarLabel = new Label("Click to change avatar");
        changeAvatarLabel.setStyle("-fx-text-fill: #7289da; -fx-font-size: 12px; -fx-cursor: hand;");

        final File[] selectedAvatar = {null};
        javafx.event.EventHandler<javafx.scene.input.MouseEvent> pickAvatar = e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose avatar");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            File file = chooser.showOpenDialog(dialog.getOwner());
            if (file != null && file.length() <= 3 * 1024 * 1024) {
                selectedAvatar[0] = file;
                try {
                    Image preview = new Image(file.toURI().toString(), 80, 80, true, true);
                    ImageView iv = new ImageView(preview);
                    iv.setFitWidth(80);
                    iv.setFitHeight(80);
                    iv.setPreserveRatio(true);
                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(40, 40, 40);
                    iv.setClip(clip);
                    avatarPreview.getChildren().setAll(iv);
                } catch (Exception ignored) {}
                changeAvatarLabel.setText(file.getName());
            } else if (file != null) {
                showAlert(Alert.AlertType.ERROR, "Avatar must be under 3 MB.");
            }
        };
        avatarPreview.setOnMouseClicked(pickAvatar);
        changeAvatarLabel.setOnMouseClicked(pickAvatar);

        VBox avatarSection = new VBox(6, avatarPreview, changeAvatarLabel);
        avatarSection.setAlignment(Pos.CENTER);

        Label nameLabel = new Label("DISPLAY NAME");
        nameLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 11px; -fx-font-weight: bold;");
        TextField nameField = new TextField(currentDisplayName);
        nameField.setStyle("-fx-background-color: #202225; -fx-text-fill: #dcddde; " +
                "-fx-background-radius: 4; -fx-padding: 8 12;");

        Label usernameLabel = new Label("USERNAME");
        usernameLabel.setStyle("-fx-text-fill: #8e9297; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label usernameValue = new Label("@" + (knownUsers.containsKey(currentUserId)
                ? knownUsers.get(currentUserId).getUsername() : currentDisplayName));
        usernameValue.setStyle("-fx-text-fill: #72767d; -fx-font-size: 14px;");

        content.getChildren().addAll(avatarSection, nameLabel, nameField, usernameLabel, usernameValue);
        pane.setContent(content);

        pane.lookupButton(ButtonType.APPLY).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Display name cannot be empty.");
                return;
            }
            com.micord.common.protocol.request.UpdateProfileRequest req =
                    new com.micord.common.protocol.request.UpdateProfileRequest();
            req.setDisplayName(newName);
            if (selectedAvatar[0] != null) {
                try {
                    byte[] data = Files.readAllBytes(selectedAvatar[0].toPath());
                    req.setAvatarData(java.util.Base64.getEncoder().encodeToString(data));
                    String mime = Files.probeContentType(selectedAvatar[0].toPath());
                    req.setAvatarMimeType(mime != null ? mime : "image/png");
                } catch (IOException ignored) {}
            }
            connection.send(JsonUtil.wrap(MessageType.UPDATE_PROFILE_REQUEST, req));
            dialog.close();
        });

        dialog.showAndWait();
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
        } else if ("GIF".equalsIgnoreCase(n.getMessageType())) {
            m.setMessageType(Message.MessageType.GIF);
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

        String senderName = m.getSenderName() == null ? "Unknown" : m.getSenderName();
        StackPane msgAvatar = createAvatarNode(m.getSenderId(), senderName, 36);
        messageAvatarNodes.computeIfAbsent(m.getSenderId(), ignored -> new ArrayList<>()).add(msgAvatar);

        Label nameLabel = new Label(senderName);
        nameLabel.getStyleClass().add("sender-name");
        nameLabel.setStyle(m.getSenderId() == currentUserId
                ? "-fx-text-fill: #7289da;"
                : "-fx-text-fill: #ffffff;");
        Label timeLabel = new Label(formatTime(timestamp));
        timeLabel.getStyleClass().add("timestamp");

        Label editedMarker = new Label("(edited)");
        editedMarker.setStyle("-fx-text-fill: #72767d; -fx-font-size: 10px;");
        editedMarker.setVisible(m.isEdited());
        editedMarker.setManaged(m.isEdited());

        HBox header = new HBox(8, nameLabel, timeLabel, editedMarker);
        header.setAlignment(Pos.BASELINE_LEFT);

        Node body;
        Label textLabel = null;
        if (m.getMessageType() == Message.MessageType.GIF && m.getContent() != null && !m.getContent().isBlank()) {
            body = renderGifMessage(m.getContent());
        } else if (m.getAttachment() != null) {
            body = renderAttachment(m.getAttachment());
        } else {
            Label text = new Label(m.getContent() == null ? "" : m.getContent());
            text.getStyleClass().add("message-content");
            text.setWrapText(true);
            text.setMaxWidth(Double.MAX_VALUE);
            body = text;
            textLabel = text;
        }

        FlowPane reactionRow = new FlowPane(6, 4);
        reactionRow.setVisible(false);
        reactionRow.setManaged(false);

        VBox contentColumn = new VBox(2, header, body, reactionRow);
        contentColumn.getStyleClass().add("message-bubble");
        HBox.setHgrow(contentColumn, javafx.scene.layout.Priority.ALWAYS);

        HBox container = new HBox(10, msgAvatar, contentColumn);
        container.setAlignment(Pos.TOP_LEFT);
        container.setStyle("-fx-padding: 4 16 4 16;");

        MessageBubble bubble = new MessageBubble(container, reactionRow, m.getId());
        bubble.senderId = m.getSenderId();
        bubble.textLabel = textLabel;
        bubble.editedMarker = editedMarker;
        bubble.replaceReactions(m.getReactions());

        boolean isOwn = m.getSenderId() == currentUserId;
        boolean editable = isOwn && textLabel != null; // only own TEXT messages can be edited
        container.setOnMouseClicked(ev -> {
            if (ev.getButton() != MouseButton.SECONDARY) return;
            if (!isOwn) {
                showReactionMenu(bubble, ev.getScreenX(), ev.getScreenY());
                return;
            }
            // Own message: offer React + Edit/Delete.
            ContextMenu menu = new ContextMenu();
            MenuItem react = new MenuItem("Add reaction…");
            react.setOnAction(e -> showReactionMenu(bubble, ev.getScreenX(), ev.getScreenY()));
            menu.getItems().add(react);
            if (editable) {
                MenuItem edit = new MenuItem("Edit");
                edit.setOnAction(e -> handleEditMessage(bubble));
                menu.getItems().add(edit);
            }
            MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(e -> handleDeleteMessage(bubble.messageId));
            menu.getItems().add(delete);
            menu.show(container, ev.getScreenX(), ev.getScreenY());
        });
        return bubble;
    }

    private void handleEditMessage(MessageBubble bubble) {
        if (bubble.textLabel == null) return;
        TextInputDialog dialog = new TextInputDialog(bubble.textLabel.getText());
        dialog.setTitle("Edit message");
        dialog.setHeaderText("Edit your message");
        dialog.setContentText("Message:");
        dialog.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(content ->
                connection.send(JsonUtil.wrap(MessageType.EDIT_MESSAGE_REQUEST,
                        new EditMessageRequest(bubble.messageId, content))));
    }

    private void handleDeleteMessage(long messageId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this message?", ButtonType.OK, ButtonType.CANCEL);
        confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b ->
                connection.send(JsonUtil.wrap(MessageType.DELETE_MESSAGE_REQUEST,
                        new DeleteMessageRequest(messageId))));
    }

    private Node renderGifMessage(String gifUrl) {
        Image gif = new Image(gifUrl, 320, 0, true, true, true);
        ImageView iv = new ImageView(gif);
        iv.setFitWidth(320);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setStyle("-fx-cursor: hand;");
        iv.setOnMouseClicked(e -> {
            try { Desktop.getDesktop().browse(new java.net.URI(gifUrl)); } catch (Exception ignored) {}
        });
        VBox container = new VBox(4, iv);
        Label poweredBy = new Label("via GIPHY");
        poweredBy.setStyle("-fx-text-fill: #72767d; -fx-font-size: 10px;");
        container.getChildren().add(poweredBy);
        return container;
    }

    private Node renderAttachment(FileAttachment att) {
        String mime = att.getMimeType();
        if (mime != null && mime.startsWith("image/")) return renderImageAttachment(att);
        if (mime != null && mime.startsWith("video/")) return renderVideoAttachment(att);
        return renderGenericAttachment(att);
    }

    private Node renderImageAttachment(FileAttachment att) {
        VBox container = new VBox(4);
        container.getStyleClass().addAll("message-content", "media-container");

        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(360);
        imageView.setSmooth(true);
        imageView.setCursor(javafx.scene.Cursor.HAND);
        imageView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) openFileExternally(att);
        });

        StackPane imageHolder = new StackPane();
        Label loading = new Label("Loading image…");
        loading.setStyle("-fx-text-fill: #72767d; -fx-font-style: italic;");
        imageHolder.getChildren().add(loading);
        imageHolder.setMinHeight(60);
        imageHolder.setAlignment(Pos.CENTER_LEFT);

        FileTransferClient.getInstance().downloadForPreview(att.getId(), att.getFileName(), path -> {
            Image image = new Image(path.toUri().toString(), 360, 0, true, true, true);
            imageView.setImage(image);
            imageHolder.getChildren().setAll(imageView);
        });

        HBox info = new HBox(8);
        info.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(att.getFileName() + " (" + formatBytes(att.getFileSize()) + ")");
        nameLabel.setStyle("-fx-text-fill: #72767d; -fx-font-size: 11px;");
        Button save = new Button("Save");
        save.getStyleClass().add("button-link");
        save.setStyle("-fx-font-size: 11px;");
        save.setOnAction(e -> startDownload(att));
        info.getChildren().addAll(nameLabel, save);

        container.getChildren().addAll(imageHolder, info);
        return container;
    }

    private Node renderVideoAttachment(FileAttachment att) {
        VBox container = new VBox(4);
        container.getStyleClass().addAll("message-content", "media-container");

        HBox preview = new HBox(12);
        preview.getStyleClass().add("video-placeholder");
        preview.setAlignment(Pos.CENTER_LEFT);
        preview.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) openFileExternally(att);
        });

        Label icon = new Label("🎬");
        icon.setStyle("-fx-font-size: 28px;");
        VBox details = new VBox(2);
        Label name = new Label(att.getFileName());
        name.setStyle("-fx-text-fill: #00b0f4; -fx-font-weight: bold; -fx-font-size: 14px;");
        Label size = new Label(formatBytes(att.getFileSize()) + " — Click to play");
        size.setStyle("-fx-text-fill: #72767d; -fx-font-size: 12px;");
        details.getChildren().addAll(name, size);
        preview.getChildren().addAll(icon, details);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        Button save = new Button("Save");
        save.getStyleClass().add("button-link");
        save.setStyle("-fx-font-size: 11px;");
        save.setOnAction(e -> startDownload(att));
        actions.getChildren().add(save);

        container.getChildren().addAll(preview, actions);
        return container;
    }

    private Node renderGenericAttachment(FileAttachment att) {
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

    private void openFileExternally(FileAttachment att) {
        FileTransferClient.getInstance().downloadForPreview(att.getId(), att.getFileName(), path -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    new Thread(() -> {
                        try { Desktop.getDesktop().open(path.toFile()); }
                        catch (IOException ignored) {}
                    }).start();
                }
            } catch (Exception ignored) {}
        });
    }

    private void showReactionMenu(MessageBubble bubble, double x, double y) {
        if (emojiPopup != null) emojiPopup.hide();

        emojiPopup = new javafx.stage.Popup();
        emojiPopup.setAutoHide(true);

        HBox bar = new HBox(2);
        bar.setStyle("-fx-background-color: #18191c; -fx-background-radius: 8; -fx-padding: 4 6;");
        bar.setAlignment(Pos.CENTER);
        for (String emoji : EMOJI_PALETTE) {
            Button btn = new Button();
            ImageView iv = emojiImageView(emoji, 24);
            if (iv != null) {
                btn.setGraphic(iv);
            } else {
                btn.setText(emoji);
            }
            btn.setStyle("-fx-background-color: transparent; -fx-padding: 4 6; -fx-cursor: hand; -fx-background-radius: 6;");
            btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-background-color: #2f3136;"));
            btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("-fx-background-color: #2f3136;", "")));
            btn.setOnAction(e -> {
                emojiPopup.hide();
                toggleReaction(bubble.messageId, emoji);
            });
            bar.getChildren().add(btn);
        }
        emojiPopup.getContent().add(bar);
        emojiPopup.show(SceneManager.getPrimaryStage(), x, y - 50);
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
        LocalDateTime when = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        LocalDate today = LocalDate.now();
        LocalDate messageDate = when.toLocalDate();
        if (messageDate.equals(today)) {
            return "Today " + when.format(TIME_FMT);
        }
        if (messageDate.equals(today.minusDays(1))) {
            return "Yesterday " + when.format(TIME_FMT);
        }
        return when.format(DATE_TIME_FMT);
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
        final Node root;
        final FlowPane reactionRow;
        final long messageId;
        final Map<String, java.util.LinkedHashSet<Long>> reactions = new LinkedHashMap<>();
        final Map<String, Button> badges = new LinkedHashMap<>();
        long senderId;
        Label textLabel;      // non-null only for editable TEXT messages
        Label editedMarker;   // faint "(edited)" label in the header

        MessageBubble(Node root, FlowPane reactionRow, long messageId) {
            this.root = root;
            this.reactionRow = reactionRow;
            this.messageId = messageId;
        }

        void applyEdit(String content) {
            if (textLabel != null) textLabel.setText(content);
            if (editedMarker != null) {
                editedMarker.setVisible(true);
                editedMarker.setManaged(true);
            }
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
            if (badge == null) {
                badge = new Button(String.valueOf(users.size()));
                ImageView iv = emojiImageView(emoji, 14);
                if (iv != null) {
                    badge.setGraphic(iv);
                } else {
                    badge.setText(emoji + " " + users.size());
                }
                badge.getStyleClass().add("reaction-badge");
                badge.setOnAction(e -> toggleReaction(messageId, emoji));
                badges.put(emoji, badge);
                reactionRow.getChildren().add(badge);
            } else {
                badge.setText(badge.getGraphic() != null ? String.valueOf(users.size()) : emoji + " " + users.size());
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
        String displayName;
        DmEntry(long userId, String displayName) {
            this.userId = userId;
            this.displayName = displayName;
        }
        @Override public String toString() { return displayName; }
    }
}
