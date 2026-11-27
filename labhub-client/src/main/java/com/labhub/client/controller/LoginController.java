package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.app.ViewManager;
import com.labhub.client.network.ApiClient;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.UserDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the login screen.
 *
 * <p>Network calls run on a background thread via {@link Thread#ofVirtual()}.
 * UI updates are always dispatched back to the JavaFX Application Thread via
 * {@link Platform#runLater(Runnable)}.
 */
public class LoginController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField      usernameField;
    @FXML private PasswordField  passwordField;
    @FXML private Button         loginButton;
    @FXML private HBox           errorBox;
    @FXML private Label          errorLabel;
    @FXML private HBox           loadingBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Focus username field when the screen loads
        Platform.runLater(() -> usernameField.requestFocus());
    }

    /**
     * Triggered by the Sign In button or Enter key in either field.
     */
    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Client-side pre-validation (server also validates)
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter your username and password.");
            return;
        }

        setLoading(true);

        // Run network call on a virtual thread to keep the UI responsive
        Thread.ofVirtual().name("login-thread").start(() -> {
            try {
                ApiClient api = AppContext.get().getApiClient();
                UserDto user = api.login(username, password);
                AppContext.get().setCurrentUser(user);
                LOG.info("Login successful: {} ({})", user.getUsername(), user.getRoles());

                Platform.runLater(() -> {
                    setLoading(false);
                    ViewManager.get().showMainWindow(ViewManager.get().getPrimaryStage());
                });

            } catch (ApiException e) {
                LOG.warn("Login failed: {} - {}", e.getCode(), e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    if (e.isNetworkError()) {
                        showError("Cannot reach the server. Please try again.");
                    } else {
                        showError(e.getMessage());
                    }
                    passwordField.clear();
                    passwordField.requestFocus();
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
        if (loading) {
            hideError();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
    }

    private void hideError() {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }
}
