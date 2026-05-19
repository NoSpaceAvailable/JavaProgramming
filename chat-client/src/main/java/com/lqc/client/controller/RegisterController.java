package com.lqc.client.controller;

import com.lqc.client.net.MessageListener;
import com.lqc.client.net.ServerConnection;
import com.lqc.client.util.ClientConfig;
import com.lqc.client.util.SceneManager;
import com.lqc.common.protocol.MessageType;
import com.lqc.common.protocol.ProtocolMessage;
import com.lqc.common.protocol.request.RegisterRequest;
import com.lqc.common.protocol.response.RegisterResponse;
import com.lqc.common.util.JsonUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController implements MessageListener {
    @FXML private TextField usernameField;
    @FXML private TextField displayNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button registerButton;

    private final ServerConnection connection = ServerConnection.getInstance();

    @FXML
    public void initialize() {
        connection.addListener(this);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String displayName = displayNameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username and password are required");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        if (displayName.isEmpty()) {
            displayName = username;
        }

        registerButton.setDisable(true);
        hideMessages();

        if (!connection.isConnected()) {
            try {
                connection.connect(ClientConfig.getServerHost(), ClientConfig.getServerPort());
            } catch (Exception e) {
                showError("Cannot connect to server: " + e.getMessage());
                registerButton.setDisable(false);
                return;
            }
        }

        connection.send(JsonUtil.wrap(MessageType.REGISTER_REQUEST,
                new RegisterRequest(username, password, displayName)));
    }

    @FXML
    private void switchToLogin() {
        connection.removeListener(this);
        SceneManager.switchTo("login");
    }

    @Override
    public void onMessageReceived(ProtocolMessage message) {
        if (message.getType() == MessageType.REGISTER_RESPONSE) {
            RegisterResponse response = JsonUtil.fromJson(message.getPayload(), RegisterResponse.class);
            registerButton.setDisable(false);
            if (response.isSuccess()) {
                showSuccess("Account created! You can now log in.");
            } else {
                showError(response.getMessage());
            }
        }
    }

    private void showError(String msg) {
        successLabel.setVisible(false);
        successLabel.setManaged(false);
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}
