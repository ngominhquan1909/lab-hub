package com.labhub.client.app;

import com.labhub.client.network.ServerConnection;
import com.labhub.client.network.ApiClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * LabHub JavaFX client application entry point.
 *
 * <p>Startup sequence:
 * <ol>
 *   <li>Load client.properties.</li>
 *   <li>Connect to the LabHub server.</li>
 *   <li>Wire {@link ApiClient} and store in {@link AppContext}.</li>
 *   <li>Display the login screen.</li>
 * </ol>
 *
 * <p>If the server is unreachable, a connection error dialog is shown and the
 * application exits cleanly.
 *
 * <p>Launch: {@code mvn -pl labhub-client javafx:run}
 * or via the generated fat jar after packaging.
 */
public class LabHubClientApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(LabHubClientApplication.class);

    @Override
    public void start(Stage primaryStage) {
        LOG.info("=== LabHub Client starting ===");

        // Store stage reference
        ViewManager.get().setPrimaryStage(primaryStage);

        // Load configuration
        Properties config = loadClientConfig();
        String host          = config.getProperty("server.host",              "localhost");
        int    port          = Integer.parseInt(config.getProperty("server.port",          "9090"));
        int    connectTimeout = Integer.parseInt(config.getProperty("server.connect.timeout.ms", "5000"));
        int    readTimeout    = Integer.parseInt(config.getProperty("server.read.timeout.ms",    "15000"));

        // Connect to server
        ServerConnection connection = new ServerConnection(host, port, connectTimeout, readTimeout);
        try {
            connection.connect();
        } catch (IOException e) {
            LOG.error("Failed to connect to server at {}:{} - {}", host, port, e.getMessage());
            showConnectionError(host, port, e.getMessage());
            return;
        }

        // Store in context
        ApiClient apiClient = new ApiClient(connection);
        AppContext.get().setConnection(connection);
        AppContext.get().setApiClient(apiClient);

        // Setup shutdown on window close
        primaryStage.setOnCloseRequest(event -> {
            LOG.info("Window closed - cleaning up.");
            AppContext.get().getConnection().close();
            Platform.exit();
        });

        // Show login
        ViewManager.get().showLoginScreen();
        LOG.info("Login screen displayed.");
    }

    private Properties loadClientConfig() {
        Properties props = new Properties();

        // Try working directory first
        Path workingDir = Paths.get("client.properties");
        if (Files.exists(workingDir)) {
            try (InputStream is = Files.newInputStream(workingDir)) {
                props.load(is);
                LOG.info("Client config loaded from: {}", workingDir.toAbsolutePath());
                return props;
            } catch (IOException e) {
                LOG.warn("Failed to read client.properties: {}", e.getMessage());
            }
        }

        // Classpath fallback
        try (InputStream is = getClass().getResourceAsStream("/client.properties")) {
            if (is != null) {
                props.load(is);
                LOG.info("Client config loaded from classpath.");
            }
        } catch (IOException e) {
            LOG.warn("No client.properties found - using defaults.");
        }
        return props;
    }

    private void showConnectionError(String host, int port, String reason) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Connection Failed");
            alert.setHeaderText("Cannot connect to LabHub Server");
            alert.setContentText("Server: " + host + ":" + port
                    + "\n\nError: " + reason
                    + "\n\nPlease ensure the server is running and check client.properties.");
            alert.showAndWait();
            Platform.exit();
        });
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
