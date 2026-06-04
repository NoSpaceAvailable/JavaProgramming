package com.micord.client.gif;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.function.Consumer;

public final class GifPicker {
    private final GiphyService giphyService;
    private long requestVersion;

    public GifPicker(GiphyService giphyService) {
        this.giphyService = giphyService;
    }

    public void show(Stage owner, Consumer<GifResult> onSelected) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("Choose a GIF");

        TextField searchField = new TextField();
        searchField.setPromptText("Search GIFs...");
        searchField.setStyle("-fx-background-color: #1e1f22; -fx-text-fill: white; " +
                "-fx-prompt-text-fill: #949ba4; -fx-background-radius: 6; -fx-padding: 8;");

        TilePane gifGrid = new TilePane();
        gifGrid.setHgap(6);
        gifGrid.setVgap(6);
        gifGrid.setPadding(new Insets(8));
        gifGrid.setPrefColumns(3);

        ScrollPane scrollPane = new ScrollPane(gifGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(500, 400);
        scrollPane.setStyle("-fx-background: #313338; -fx-background-color: transparent;");

        Label attribution = new Label("Powered by GIPHY");
        attribution.setStyle("-fx-text-fill: #b5bac1; -fx-font-size: 11px;");

        VBox root = new VBox(8, searchField, scrollPane, attribution);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #313338;");

        PauseTransition debounce = new PauseTransition(Duration.millis(500));
        searchField.textProperty().addListener((obs, oldT, newT) -> {
            debounce.stop();
            debounce.setOnFinished(e -> loadGifs(newT, gifGrid, stage, onSelected));
            debounce.playFromStart();
        });
        searchField.setOnAction(e -> {
            debounce.stop();
            loadGifs(searchField.getText(), gifGrid, stage, onSelected);
        });

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
        Platform.runLater(searchField::requestFocus);
        loadGifs("", gifGrid, stage, onSelected);
    }

    private void loadGifs(String query, TilePane grid, Stage stage, Consumer<GifResult> onSelected) {
        long version = ++requestVersion;
        Platform.runLater(() -> {
            grid.getChildren().clear();
            Label loading = new Label("Loading...");
            loading.setStyle("-fx-text-fill: #dbdee1;");
            grid.getChildren().add(loading);
        });

        giphyService.searchGifs(query, 24)
                .thenAccept(gifs -> Platform.runLater(() -> {
                    if (version != requestVersion) return;
                    grid.getChildren().clear();
                    if (gifs.isEmpty()) {
                        Label empty = new Label("No GIFs found.");
                        empty.setStyle("-fx-text-fill: #dbdee1;");
                        grid.getChildren().add(empty);
                        return;
                    }
                    for (GifResult gif : gifs) {
                        Image img = new Image(gif.previewUrl(), 150, 0, true, true, true);
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(150);
                        iv.setPreserveRatio(true);
                        iv.setSmooth(true);

                        Button btn = new Button();
                        btn.setGraphic(iv);
                        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; " +
                                "-fx-cursor: hand; -fx-background-radius: 8;");
                        btn.setOnAction(e -> {
                            onSelected.accept(gif);
                            stage.close();
                        });
                        grid.getChildren().add(btn);
                    }
                }))
                .exceptionally(error -> {
                    Platform.runLater(() -> {
                        if (version != requestVersion) return;
                        grid.getChildren().clear();
                        Label err = new Label("Failed to load GIFs.");
                        err.setStyle("-fx-text-fill: #f04747;");
                        grid.getChildren().add(err);
                    });
                    return null;
                });
    }
}
