package com.micord.client.controller;

import com.micord.client.net.MessageListener;
import com.micord.client.net.ServerConnection;
import com.micord.client.util.ClientConfig;
import com.micord.client.util.SceneManager;
import com.micord.common.protocol.MessageType;
import com.micord.common.protocol.ProtocolMessage;
import com.micord.common.protocol.request.LoginRequest;
import com.micord.common.protocol.response.LoginResponse;
import com.micord.common.util.JsonUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Base64;
import java.util.prefs.Preferences;

public class LoginController implements MessageListener {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private CheckBox rememberMeCheck;

    private static final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
    private static final String PREF_USERNAME = "saved_username";
    private static final String PREF_PASSWORD = "saved_password";
    private static final String PREF_REMEMBER = "remember_me";

    private final ServerConnection connection = ServerConnection.getInstance();
    private boolean autoLoginAttempted;
    private static boolean suppressAutoLogin;

    @FXML
    public void initialize() {
        connection.addListener(this);
        boolean remembered = prefs.getBoolean(PREF_REMEMBER, false);
        rememberMeCheck.setSelected(remembered);
        if (remembered && !suppressAutoLogin) {
            String savedUser = prefs.get(PREF_USERNAME, "");
            String savedPass = decodePass(prefs.get(PREF_PASSWORD, ""));
            if (!savedUser.isEmpty() && !savedPass.isEmpty()) {
                usernameField.setText(savedUser);
                passwordField.setText(savedPass);
                Platform.runLater(this::autoLogin);
            }
        }
    }

    private void autoLogin() {
        if (autoLoginAttempted) return;
        autoLoginAttempted = true;
        handleLogin();
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
                connection.connect(ClientConfig.getServerHost(), ClientConfig.getServerPort());
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
                saveOrClearCredentials();
                connection.removeListener(this);
                MainChatController controller = SceneManager.switchToAndGetController("main", 1200, 800);
                controller.initWithUser(response.getUserId(), response.getDisplayName(),
                        response.getRooms(), response.getRecentDmPeers());
            } else {
                showError(response.getMessage());
                loginButton.setDisable(false);
            }
        } else if (message.getType() == MessageType.ERROR_RESPONSE) {
            showError("Server error — please try again");
            loginButton.setDisable(false);
        }
    }

    private void saveOrClearCredentials() {
        if (rememberMeCheck.isSelected()) {
            prefs.put(PREF_USERNAME, usernameField.getText().trim());
            prefs.put(PREF_PASSWORD, encodePass(passwordField.getText()));
            prefs.putBoolean(PREF_REMEMBER, true);
        } else {
            prefs.remove(PREF_USERNAME);
            prefs.remove(PREF_PASSWORD);
            prefs.putBoolean(PREF_REMEMBER, false);
        }
    }

    private static String encodePass(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String decodePass(String encoded) {
        if (encoded.isEmpty()) return "";
        try {
            return new String(Base64.getDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static void disableAutoLogin() {
        suppressAutoLogin = true;
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
