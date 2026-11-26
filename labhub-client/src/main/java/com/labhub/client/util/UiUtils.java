package com.labhub.client.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/**
 * Shared utility methods for JavaFX dialogs.
 * All methods must be called from the JavaFX Application Thread.
 */
public final class UiUtils {

    private UiUtils() { }

    /**
     * Shows a modal error dialog.
     *
     * @param title   dialog title
     * @param message error message
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a modal informational dialog.
     *
     * @param title   dialog title
     * @param message information message
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a confirmation dialog and returns true if the user clicked OK.
     *
     * @param title   dialog title
     * @param message confirmation question
     * @return true if confirmed
     */
    public static boolean showConfirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Shows a text input dialog and returns the entered text.
     *
     * @param title        dialog title
     * @param prompt       label shown above the text field
     * @param defaultValue initial value in the field
     * @return entered text, or empty if cancelled
     */
    public static Optional<String> showTextInput(String title, String prompt, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue != null ? defaultValue : "");
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);
        return dialog.showAndWait();
    }
}
