package com.micord.client;

import com.micord.client.net.ServerConnection;
import com.micord.client.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ChatClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("Micord");
        primaryStage.setMinWidth(720);
        primaryStage.setMinHeight(540);
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
