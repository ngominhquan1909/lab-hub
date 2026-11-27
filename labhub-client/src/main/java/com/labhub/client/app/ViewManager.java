package com.labhub.client.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

/**
 * Manages scene and view transitions for the JavaFX application.
 *
 * <p>All FXML files are located in {@code /com/labhub/client/view/} on the classpath.
 * Controllers are instantiated by the FXMLLoader (using the {@code fx:controller} attribute).
 *
 * <p>Navigation pattern: {@link #showMainWindow(Stage)} replaces the stage scene
 * with the main layout. {@link #showContentPane(Pane, String)} swaps the center
 * content area of the main layout without recreating the sidebar.
 */
public final class ViewManager {

    private static final Logger LOG = LoggerFactory.getLogger(ViewManager.class);
    private static final String VIEW_BASE = "/com/labhub/client/view/";

    private static final ViewManager INSTANCE = new ViewManager();

    private Stage primaryStage;

    private ViewManager() { }

    /**
     * Returns the singleton instance.
     *
     * @return view manager
     */
    public static ViewManager get() {
        return INSTANCE;
    }

    /**
     * Stores a reference to the primary stage (called once at startup).
     *
     * @param stage the application's primary stage
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Displays the login screen (replaces the current scene).
     */
    public void showLoginScreen() {
        loadScene("LoginView.fxml", "LabHub — Sign In", 460, 540);
    }

    /**
     * Displays the main application window (replaces the current scene).
     *
     * @param stage the stage to populate
     */
    public void showMainWindow(Stage stage) {
        loadScene("MainView.fxml", "LabHub — Laboratory Management System", 1280, 820);
        stage.setMinWidth(960);
        stage.setMinHeight(640);
    }

    /**
     * Swaps the center content of the main window with the given FXML view.
     *
     * @param contentPane the center container pane from the main layout
     * @param fxmlName    the FXML filename (e.g. "DashboardView.fxml")
     * @return the loaded controller (may be cast by the caller)
     */
    public Object showContentPane(Pane contentPane, String fxmlName) {
        try {
            FXMLLoader loader = createLoader(fxmlName);
            Parent view = loader.load();
            contentPane.getChildren().setAll(view);
            // Make the view fill the container
            if (view instanceof javafx.scene.layout.Region region) {
                region.prefWidthProperty().bind(contentPane.widthProperty());
                region.prefHeightProperty().bind(contentPane.heightProperty());
            }
            return loader.getController();
        } catch (IOException e) {
            LOG.error("Failed to load content pane '{}': {}", fxmlName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Opens a modal dialog with the given FXML.
     *
     * @param fxmlName dialog FXML filename
     * @param title    dialog title
     * @return the loaded controller
     */
    public Object showDialog(String fxmlName, String title) {
        return showDialog(fxmlName, title, null);
    }

    /**
     * Opens a modal dialog with the given FXML, allowing the caller to configure the
     * controller (e.g. pass in data to display/edit) before the dialog is shown.
     *
     * @param fxmlName   dialog FXML filename
     * @param title      dialog title
     * @param beforeShow optional callback invoked with the loaded controller before
     *                   {@code showAndWait()}; may be null
     * @return the loaded controller
     */
    public Object showDialog(String fxmlName, String title,
                              java.util.function.Consumer<Object> beforeShow) {
        try {
            FXMLLoader loader = createLoader(fxmlName);
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.setTitle(title);
            Scene scene = new Scene(root);
            URL css = getClass().getResource("/com/labhub/client/css/labhub.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            dialog.setScene(scene);
            dialog.initOwner(primaryStage);
            dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
            Object controller = loader.getController();
            if (beforeShow != null) {
                beforeShow.accept(controller);
            }
            dialog.showAndWait();
            return controller;
        } catch (IOException e) {
            LOG.error("Failed to open dialog '{}': {}", fxmlName, e.getMessage(), e);
            return null;
        }
    }

    private void loadScene(String fxmlName, String title, double width, double height) {
        try {
            FXMLLoader loader = createLoader(fxmlName);
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            // Load global stylesheet
            URL css = getClass().getResource("/com/labhub/client/css/labhub.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
        } catch (IOException e) {
            LOG.error("Failed to load scene '{}': {}", fxmlName, e.getMessage(), e);
            throw new RuntimeException("Cannot load view: " + fxmlName, e);
        }
    }

    private FXMLLoader createLoader(String fxmlName) {
        URL url = getClass().getResource(VIEW_BASE + fxmlName);
        if (url == null) {
            throw new RuntimeException("FXML not found on classpath: " + VIEW_BASE + fxmlName);
        }
        return new FXMLLoader(url);
    }
}
