package com.micord.client.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** Builds a custom dark title bar and wraps it around a scene's root content. */
public final class TitleBar {

    private TitleBar() {}

    private static Image cachedLogo;

    /** Wraps {@code content} in a VBox with a draggable dark title bar at the top. */
    public static Parent wrap(Stage stage, Parent content) {
        HBox bar = new HBox();
        bar.getStyleClass().add("title-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 8, 0, 10));

        ImageView logo = new ImageView();
        if (cachedLogo == null) {
            var stream = TitleBar.class.getResourceAsStream("/images/logo.png");
            if (stream != null) cachedLogo = new Image(stream);
        }
        if (cachedLogo != null) {
            logo.setImage(cachedLogo);
            logo.setFitHeight(20);
            logo.setFitWidth(20);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
        }

        Label title = new Label("Micord");
        title.getStyleClass().add("title-bar-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minBtn = barButton("–");
        minBtn.setOnAction(e -> stage.setIconified(true));

        Button maxBtn = barButton("☐");

        Button closeBtn = barButton("✕");
        closeBtn.getStyleClass().add("title-bar-close");
        closeBtn.setOnAction(e -> stage.close());

        if (cachedLogo != null) {
            bar.getChildren().addAll(logo, title, spacer, minBtn, maxBtn, closeBtn);
        } else {
            bar.getChildren().addAll(title, spacer, minBtn, maxBtn, closeBtn);
        }

        // Manual maximize: setMaximized() on an UNDECORATED stage on Windows leaves
        // X/Y at the previous (dragged) position, so the window ends up offset and
        // covering the taskbar. We track our own toggle state and resize to the
        // current screen's *visual* bounds (which exclude the taskbar) instead.
        final double[] savedGeometry = {-1, -1, -1, -1}; // x, y, w, h
        final boolean[] isMaxed = {false};
        Runnable toggleMaximize = () -> {
            if (!isMaxed[0]) {
                savedGeometry[0] = stage.getX();
                savedGeometry[1] = stage.getY();
                savedGeometry[2] = stage.getWidth();
                savedGeometry[3] = stage.getHeight();
                Rectangle2D vb = currentScreenBounds(stage);
                stage.setX(vb.getMinX());
                stage.setY(vb.getMinY());
                stage.setWidth(vb.getWidth());
                stage.setHeight(vb.getHeight());
                isMaxed[0] = true;
            } else {
                if (savedGeometry[2] > 0) {
                    stage.setX(savedGeometry[0]);
                    stage.setY(savedGeometry[1]);
                    stage.setWidth(savedGeometry[2]);
                    stage.setHeight(savedGeometry[3]);
                }
                isMaxed[0] = false;
            }
        };
        maxBtn.setOnAction(e -> toggleMaximize.run());

        // Drag + double-click to maximize.
        final double[] offset = {0, 0};
        bar.setOnMousePressed((MouseEvent e) -> {
            offset[0] = e.getScreenX() - stage.getX();
            offset[1] = e.getScreenY() - stage.getY();
        });
        bar.setOnMouseDragged(e -> {
            if (!isMaxed[0]) {
                stage.setX(e.getScreenX() - offset[0]);
                stage.setY(e.getScreenY() - offset[1]);
            }
        });
        bar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getY() < 32) {
                toggleMaximize.run();
            }
        });

        VBox wrapper = new VBox(bar, content);
        wrapper.getStyleClass().add("title-bar-wrapper");
        VBox.setVgrow(content, Priority.ALWAYS);

        installResizeGrip(stage, wrapper, isMaxed);

        return wrapper;
    }

    private static Button barButton(String glyph) {
        Button b = new Button(glyph);
        b.getStyleClass().add("title-bar-button");
        b.setFocusTraversable(false);
        return b;
    }

    /** Finds the visual bounds of whichever screen the stage's top-left sits on. */
    private static Rectangle2D currentScreenBounds(Stage stage) {
        var screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1);
        Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        return screen.getVisualBounds();
    }

    /** Adds an invisible 6px resize zone along the bottom and right edges. */
    private static void installResizeGrip(Stage stage, VBox wrapper, boolean[] isMaxed) {
        final int EDGE = 6;
        final boolean[] resizing = {false};
        final double[] start = {0, 0, 0, 0};

        wrapper.setOnMouseMoved(e -> {
            if (isMaxed[0]) {
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
            if (isMaxed[0]) return;
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
