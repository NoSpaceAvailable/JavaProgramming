package com.lqc.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class SceneManager {
    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);
    private static Stage primaryStage;
    private static final Map<String, String> fxmlMap = Map.of(
            "login", "/fxml/login.fxml",
            "register", "/fxml/register.fxml",
            "main", "/fxml/main-chat.fxml"
    );

    private SceneManager() {}

    public static void initialize(Stage stage) {
        primaryStage = stage;
    }

    public static void switchTo(String sceneName) {
        switchTo(sceneName, 900, 650);
    }

    public static void switchTo(String sceneName, double width, double height) {
        try {
            String fxmlPath = fxmlMap.get(sceneName);
            if (fxmlPath == null) {
                throw new IllegalArgumentException("Unknown scene: " + sceneName);
            }
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            String cssPath = SceneManager.class.getResource("/css/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            primaryStage.setScene(scene);
            logger.debug("Switched to scene: {}", sceneName);
        } catch (IOException e) {
            logger.error("Failed to load scene: {}", sceneName, e);
            throw new RuntimeException("Failed to load scene: " + sceneName, e);
        }
    }

    public static <T> T switchToAndGetController(String sceneName) {
        return switchToAndGetController(sceneName, 900, 650);
    }

    public static <T> T switchToAndGetController(String sceneName, double width, double height) {
        try {
            String fxmlPath = fxmlMap.get(sceneName);
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            String cssPath = SceneManager.class.getResource("/css/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
            primaryStage.setScene(scene);
            return loader.getController();
        } catch (IOException e) {
            logger.error("Failed to load scene: {}", sceneName, e);
            throw new RuntimeException("Failed to load scene: " + sceneName, e);
        }
    }

    public static Stage getPrimaryStage() { return primaryStage; }
}
