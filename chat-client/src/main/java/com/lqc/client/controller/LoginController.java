package com.lqc.client.controller;

import com.lqc.client.net.MessageListener;
import com.lqc.client.net.ServerConnection;
import com.lqc.client.util.SceneManager;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.LoginRequest;
import com.lqc.common.protocol.response.LoginResponse;
import com.lqc.common.util.JsonUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements MessageListener {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final ServerConnection connection = ServerConnection.getInstance();

    @FXML
    public void initialize() {
        connection.addListener(this);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        loginButton.setDisable(true);
        hideError();

        if (!connection.isConnected()) {
            try {
                connection.connect("localhost", 9000);
            } catch (Exception e) {
                showError("Cannot connect to server: " + e.getMessage());
                loginButton.setDisable(false);
                return;
            }
        }

        connection.send(JsonUtil.wrap(MessageType.LOGIN_REQUEST,
                new LoginRequest(username, password)));
    }

    @FXML
    private void switchToRegister() {
        connection.removeListener(this);
        SceneManager.switchTo("register");
    }

    @Override
    public void onMessageReceived(ProtocolMessage message) {
        if (message.getType() == MessageType.LOGIN_RESPONSE) {
            LoginResponse response = JsonUtil.fromJson(message.getPayload(), LoginResponse.class);
            if (response.isSuccess()) {
                connection.removeListener(this);
                MainChatController controller = SceneManager.switchToAndGetController("main", 1200, 800);
                controller.initWithUser(response.getUserId(), response.getDisplayName(), response.getRooms());
            } else {
                showError(response.getMessage());
                loginButton.setDisable(false);
            }
        } else if (message.getType() == MessageType.ERROR_RESPONSE) {
            showError("Server error — please try again");
            loginButton.setDisable(false);
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
