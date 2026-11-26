package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.EquipmentDto;
import com.labhub.common.dto.MaintenanceTicketDto;
import com.labhub.common.enums.MaintenancePriority;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the "File Maintenance Ticket" modal dialog.
 * On success, {@link #getCreatedTicket()} returns the new ticket so the caller can refresh its table.
 */
public class NewMaintenanceTicketDialogController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(NewMaintenanceTicketDialogController.class);

    @FXML private Label           errorLabel;
    @FXML private ComboBox<String> equipmentCombo;
    @FXML private ComboBox<String> priorityCombo;
    @FXML private TextArea        descriptionField;
    @FXML private HBox            loadingBox;
    @FXML private Button          saveBtn;

    private final Map<String, Long> equipmentIdsByLabel = new LinkedHashMap<>();
    private MaintenanceTicketDto createdTicket;

    /** @return the created ticket, or null if cancelled or save failed. */
    public MaintenanceTicketDto getCreatedTicket() {
        return createdTicket;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        priorityCombo.getItems().addAll(
                MaintenancePriority.LOW.name(), MaintenancePriority.MEDIUM.name(),
                MaintenancePriority.HIGH.name(), MaintenancePriority.URGENT.name());
        priorityCombo.setValue(MaintenancePriority.MEDIUM.name());

        Thread.ofVirtual().name("new-ticket-equip").start(() -> {
            try {
                var equipment = AppContext.get().getApiClient().getEquipment(null, null, 1, 300);
                Platform.runLater(() -> {
                    if (equipment.getItems() != null) {
                        for (EquipmentDto e : equipment.getItems()) {
                            if (!e.isActive()) {
                                continue;
                            }
                            String label = e.getName() + "  (" + e.getAssetCode() + ")";
                            equipmentIdsByLabel.put(label, e.getId());
                            equipmentCombo.getItems().add(label);
                        }
                    }
                });
            } catch (ApiException e) {
                LOG.warn("Failed to load equipment: {}", e.getMessage());
            }
        });
    }

    @FXML
    private void onSave() {
        hideError();
        String equipmentLabel = equipmentCombo.getValue();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();

        if (equipmentLabel == null || description.isEmpty()) {
            showError("Equipment and a description of the issue are required.");
            return;
        }

        MaintenanceTicketDto dto = new MaintenanceTicketDto();
        dto.setEquipmentId(equipmentIdsByLabel.get(equipmentLabel));
        dto.setDescription(description);
        String priorityName = priorityCombo.getValue();
        if (priorityName != null) {
            dto.setPriority(MaintenancePriority.valueOf(priorityName));
        }

        setLoading(true);
        Thread.ofVirtual().name("new-ticket-save").start(() -> {
            try {
                MaintenanceTicketDto created = AppContext.get().getApiClient()
                        .createMaintenanceTicket(dto);
                Platform.runLater(() -> {
                    createdTicket = created;
                    closeDialog();
                });
            } catch (ApiException e) {
                LOG.error("Failed to create maintenance ticket: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onCancel() {
        createdTicket = null;
        closeDialog();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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
