package com.micord.client.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Builds a custom dark title bar and wraps it around a scene's root content. */
public final class TitleBar {

    private TitleBar() {}

    /** Wraps {@code content} in a VBox with a draggable dark title bar at the top. */
    public static Parent wrap(Stage stage, Parent content) {
        HBox bar = new HBox();
        bar.getStyleClass().add("title-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 0, 12));

        Label title = new Label("Micord");
        title.getStyleClass().add("title-bar-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minBtn = barButton("–");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button maxBtn = barButton("☐");
        maxBtn.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));

        Button closeBtn = barButton("✕");
        closeBtn.getStyleClass().add("title-bar-close");
        closeBtn.setOnAction(e -> stage.close());

        bar.getChildren().addAll(title, spacer, minBtn, maxBtn, closeBtn);

        // Drag + double-click to maximize.
        final double[] offset = {0, 0};
        bar.setOnMousePressed((MouseEvent e) -> {
            offset[0] = e.getScreenX() - stage.getX();
            offset[1] = e.getScreenY() - stage.getY();
        });
        bar.setOnMouseDragged(e -> {
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - offset[0]);
                stage.setY(e.getScreenY() - offset[1]);
            }
        });
        bar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getY() < 32) {
                stage.setMaximized(!stage.isMaximized());
            }
        });

        VBox wrapper = new VBox(bar, content);
        wrapper.getStyleClass().add("title-bar-wrapper");
        VBox.setVgrow(content, Priority.ALWAYS);

        // Simple bottom-right resize grip (so users can still resize a frameless window).
        installResizeGrip(stage, wrapper);

        return wrapper;
    }

    private static Button barButton(String glyph) {
        Button b = new Button(glyph);
        b.getStyleClass().add("title-bar-button");
        b.setFocusTraversable(false);
        return b;
    }

    /** Adds an invisible 8px resize zone along the bottom and right edges. */
    private static void installResizeGrip(Stage stage, VBox wrapper) {
        final int EDGE = 6;
        final boolean[] resizing = {false};
        final double[] start = {0, 0, 0, 0};

        wrapper.setOnMouseMoved(e -> {
            if (stage.isMaximized()) {
                wrapper.setCursor(Cursor.DEFAULT);
                return;
            }
            boolean right = e.getX() >= wrapper.getWidth() - EDGE;
            boolean bottom = e.getY() >= wrapper.getHeight() - EDGE;
            if (right && bottom) wrapper.setCursor(Cursor.SE_RESIZE);
            else if (right) wrapper.setCursor(Cursor.E_RESIZE);
            else if (bottom) wrapper.setCursor(Cursor.S_RESIZE);
            else wrapper.setCursor(Cursor.DEFAULT);
        });
        wrapper.setOnMousePressed(e -> {
            if (stage.isMaximized()) return;
            boolean right = e.getX() >= wrapper.getWidth() - EDGE;
            boolean bottom = e.getY() >= wrapper.getHeight() - EDGE;
            if (right || bottom) {
                resizing[0] = true;
                start[0] = e.getScreenX();
                start[1] = e.getScreenY();
                start[2] = stage.getWidth();
                start[3] = stage.getHeight();
                e.consume();
            }
        });
        wrapper.setOnMouseDragged(e -> {
            if (!resizing[0]) return;
            double dx = e.getScreenX() - start[0];
            double dy = e.getScreenY() - start[1];
            double newW = Math.max(stage.getMinWidth(), start[2] + dx);
            double newH = Math.max(stage.getMinHeight(), start[3] + dy);
            stage.setWidth(newW);
            stage.setHeight(newH);
        });
        wrapper.setOnMouseReleased(e -> resizing[0] = false);
    }
}
