package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the "Create User Account" modal dialog (admin only).
 * On success, {@link #getCreatedUser()} returns the new account so the caller can refresh its table.
 */
public class CreateUserDialogController {

    private static final Logger LOG = LoggerFactory.getLogger(CreateUserDialogController.class);

    @FXML private Label         errorLabel;
    @FXML private TextField     usernameField;
    @FXML private TextField     fullNameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox      adminCheck;
    @FXML private CheckBox      labManagerCheck;
    @FXML private CheckBox      lecturerCheck;
    @FXML private CheckBox      studentCheck;
    @FXML private HBox          loadingBox;
    @FXML private Button        saveBtn;

    private UserDto createdUser;

    /** @return the created user account, or null if cancelled or save failed. */
    public UserDto getCreatedUser() {
        return createdUser;
    }

    @FXML
    private void onSave() {
        hideError();
        String username = textOf(usernameField);
        String fullName = textOf(fullNameField);
        String email    = textOf(emailField);
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Username, full name, email, and password are all required.");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }
        if (!email.contains("@")) {
            showError("Please enter a valid email address.");
            return;
        }

        List<UserRole> roles = new ArrayList<>();
        if (adminCheck.isSelected())      roles.add(UserRole.ADMIN);
        if (labManagerCheck.isSelected()) roles.add(UserRole.LAB_MANAGER);
        if (lecturerCheck.isSelected())   roles.add(UserRole.LECTURER);
        if (studentCheck.isSelected())    roles.add(UserRole.STUDENT);

        String phone = phoneField.getText();

        setLoading(true);
        Thread.ofVirtual().name("create-user").start(() -> {
            try {
                UserDto created = AppContext.get().getApiClient()
                        .createUser(username, email, fullName, password, phone, roles);
                Platform.runLater(() -> {
                    createdUser = created;
                    closeDialog();
                });
            } catch (ApiException e) {
                LOG.error("Failed to create user: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onCancel() {
        createdUser = null;
        closeDialog();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String textOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
        saveBtn.setDisable(loading);
    }

    private void closeDialog() {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }
}
