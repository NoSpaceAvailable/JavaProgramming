package com.lqc.client.controller;

import com.lqc.client.net.MessageListener;
import com.lqc.client.net.ServerConnection;
import com.lqc.common.model.Room;
import com.lqc.common.protocol.ProtocolMessage;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.List;

public class MainChatController implements MessageListener {
    @FXML private Label welcomeLabel;

    private long currentUserId;
    private String currentDisplayName;
    private List<Room> userRooms;

    @FXML
    public void initialize() {
        ServerConnection.getInstance().addListener(this);
    }

    public void initWithUser(long userId, String displayName, List<Room> rooms) {
        this.currentUserId = userId;
        this.currentDisplayName = displayName;
        this.userRooms = rooms;
        welcomeLabel.setText("Welcome, " + displayName + "!");
    }

    @Override
    public void onMessageReceived(ProtocolMessage message) {
        // Phase 3 will handle message routing here
    }
}
