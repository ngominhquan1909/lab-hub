package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.LabDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the "Add Laboratory" modal dialog.
 * On success, {@link #getCreatedLab()} returns the new lab so the caller can refresh its table.
 */
public class AddLabDialogController {

    private static final Logger LOG = LoggerFactory.getLogger(AddLabDialogController.class);

    @FXML private Label     errorLabel;
    @FXML private TextField labCodeField;
    @FXML private TextField nameField;
    @FXML private TextField buildingField;
    @FXML private TextField floorField;
    @FXML private TextField capacityField;
    @FXML private TextField roomTypeField;
    @FXML private TextArea  descriptionField;
    @FXML private HBox      loadingBox;
    @FXML private Button    saveBtn;

    private LabDto createdLab;

    /** @return the created lab, or null if the dialog was cancelled or save failed. */
    public LabDto getCreatedLab() {
        return createdLab;
    }

    @FXML
    private void onSave() {
        hideError();
        String labCode = labCodeField.getText() == null ? "" : labCodeField.getText().trim();
        String name    = nameField.getText() == null ? "" : nameField.getText().trim();

        if (labCode.isEmpty() || name.isEmpty()) {
            showError("Lab Code and Name are required.");
            return;
        }

        Integer floor = parseIntOrNull(floorField.getText());
        Integer capacity = parseIntOrNull(capacityField.getText());

        LabDto dto = new LabDto();
        dto.setLabCode(labCode);
        dto.setName(name);
        dto.setBuilding(blankToNull(buildingField.getText()));
        dto.setFloor(floor);
        dto.setCapacity(capacity);
        dto.setRoomType(blankToNull(roomTypeField.getText()));
        dto.setDescription(blankToNull(descriptionField.getText()));

        setLoading(true);
        Thread.ofVirtual().name("add-lab").start(() -> {
            try {
                LabDto created = AppContext.get().getApiClient().createLab(dto);
                Platform.runLater(() -> {
                    createdLab = created;
                    closeDialog();
                });
            } catch (ApiException e) {
                LOG.error("Failed to create lab: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onCancel() {
        createdLab = null;
        closeDialog();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Integer parseIntOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String blankToNull(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
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
