package com.micord.client;

import com.micord.client.net.ServerConnection;
import com.micord.client.util.SceneManager;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ChatClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("Micord");
        primaryStage.setMinWidth(720);
        primaryStage.setMinHeight(540);
        // Window/taskbar icon. Multiple sizes aren't strictly required —
        // JavaFX downscales on demand — but a single high-res PNG is enough here.
        var iconStream = ChatClientApp.class.getResourceAsStream("/images/logo.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        }
        SceneManager.initialize(primaryStage);
        SceneManager.switchTo("login");
        primaryStage.show();
    }

    @Override
    public void stop() {
        ServerConnection.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
