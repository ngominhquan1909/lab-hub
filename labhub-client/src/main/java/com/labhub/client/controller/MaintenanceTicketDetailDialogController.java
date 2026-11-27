package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.client.util.UiUtils;
import com.labhub.common.dto.MaintenanceTicketDto;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.MaintenanceStatus;
import com.labhub.common.enums.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for the Maintenance Ticket detail dialog.
 * Shows ticket details and, for managers/admins, exposes assign/resolve/close actions.
 * Use {@link com.labhub.client.app.ViewManager#showDialog(String, String, java.util.function.Consumer)}
 * with a {@code beforeShow} callback that calls {@link #setTicket(MaintenanceTicketDto)}.
 */
public class MaintenanceTicketDetailDialogController {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceTicketDetailDialogController.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML private Label     titleLabel;
    @FXML private Label     errorLabel;
    @FXML private Label     equipmentLabel;
    @FXML private Label     reporterLabel;
    @FXML private Label     priorityLabel;
    @FXML private Label     statusLabel;
    @FXML private Label     assigneeLabel;
    @FXML private Label     openedLabel;
    @FXML private Label     descriptionLabel;

    @FXML private VBox      assignBox;
    @FXML private ComboBox<String> assigneeCombo;
    @FXML private VBox      resolveBox;
    @FXML private TextArea  resolutionNotesField;
    @FXML private TextField repairCostField;
    @FXML private HBox      closeBox;

    @FXML private HBox      loadingBox;
    @FXML private Button    doneBtn;

    private final Map<String, Long> userIdsByLabel = new LinkedHashMap<>();
    private MaintenanceTicketDto ticket;
    private boolean changed = false;

    public boolean wasChanged() {
        return changed;
    }

    /**
     * Populates the dialog with the given ticket. Must be called before the dialog is shown.
     *
     * @param t the ticket to display
     */
    public void setTicket(MaintenanceTicketDto t) {
        this.ticket = t;
        boolean isManager = AppContext.get().isManager();

        titleLabel.setText("Maintenance Ticket #" + t.getId());
        equipmentLabel.setText(t.getEquipmentName() + " (" + t.getAssetCode() + ")");
        reporterLabel.setText(t.getReporterName() != null ? t.getReporterName() : "—");
        priorityLabel.setText(t.getPriority() != null ? t.getPriority().name() : "—");
        statusLabel.setText(t.getStatus() != null ? t.getStatus().name() : "—");
        assigneeLabel.setText(t.getAssigneeName() != null ? t.getAssigneeName() : "Unassigned");
        openedLabel.setText(t.getOpenedAt() != null ? t.getOpenedAt().format(DT_FMT) : "—");
        descriptionLabel.setText(t.getDescription() != null ? t.getDescription() : "");

        MaintenanceStatus status = t.getStatus();
        boolean canAssign  = isManager && status != MaintenanceStatus.RESOLVED
                && status != MaintenanceStatus.CLOSED;
        boolean canResolve = isManager && status != MaintenanceStatus.RESOLVED
                && status != MaintenanceStatus.CLOSED;
        boolean canClose   = isManager && status == MaintenanceStatus.RESOLVED;

        setVisible(assignBox, canAssign);
        setVisible(resolveBox, canResolve);
        setVisible(closeBox, canClose);

        if (canAssign) {
            loadAssignees();
        }
    }

    private void loadAssignees() {
        Thread.ofVirtual().name("ticket-assignees").start(() -> {
            try {
                var users = AppContext.get().getApiClient().getUsers(null, 1, 200);
                Platform.runLater(() -> {
                    if (users.getItems() != null) {
                        for (UserDto u : users.getItems()) {
                            if (u.getRoles() == null) continue;
                            boolean eligible = u.getRoles().contains(UserRole.ADMIN)
                                    || u.getRoles().contains(UserRole.LAB_MANAGER);
                            if (!eligible || !u.isActive()) continue;
                            String label = u.getFullName() + "  (" + u.getUsername() + ")";
                            userIdsByLabel.put(label, u.getId());
                            assigneeCombo.getItems().add(label);
                        }
                    }
                });
            } catch (ApiException e) {
                LOG.warn("Failed to load assignable users: {}", e.getMessage());
            }
        });
    }

    @FXML
    private void onAssign() {
        hideError();
        String label = assigneeCombo.getValue();
        if (label == null) {
            showError("Select a user to assign this ticket to.");
            return;
        }
        Long assigneeId = userIdsByLabel.get(label);
        setLoading(true);
        Thread.ofVirtual().name("ticket-assign").start(() -> {
            try {
                MaintenanceTicketDto updated = AppContext.get().getApiClient()
                        .assignMaintenanceTicket(ticket.getId(), assigneeId);
                Platform.runLater(() -> {
                    setLoading(false);
                    changed = true;
                    setTicket(updated);
                });
            } catch (ApiException e) {
                Platform.runLater(() -> { setLoading(false); showError(e.getMessage()); });
            }
        });
    }

    @FXML
    private void onResolve() {
        hideError();
        String notes = resolutionNotesField.getText() == null ? "" : resolutionNotesField.getText().trim();
        if (notes.isEmpty()) {
            showError("Resolution notes are required.");
            return;
        }
        BigDecimal cost = null;
        String costText = repairCostField.getText();
        if (costText != null && !costText.isBlank()) {
            try {
                cost = new BigDecimal(costText.trim());
            } catch (NumberFormatException e) {
                showError("Repair cost must be a valid number.");
                return;
            }
        }
        final BigDecimal finalCost = cost;
        setLoading(true);
        Thread.ofVirtual().name("ticket-resolve").start(() -> {
            try {
                MaintenanceTicketDto updated = AppContext.get().getApiClient()
                        .resolveMaintenanceTicket(ticket.getId(), notes, finalCost);
                Platform.runLater(() -> {
                    setLoading(false);
                    changed = true;
                    setTicket(updated);
                });
            } catch (ApiException e) {
                Platform.runLater(() -> { setLoading(false); showError(e.getMessage()); });
            }
        });
    }

    @FXML
    private void onClose() {
        boolean confirmed = UiUtils.showConfirm("Close Ticket",
                "Close this ticket and restore the equipment to AVAILABLE?");
        if (!confirmed) return;

        setLoading(true);
        Thread.ofVirtual().name("ticket-close").start(() -> {
            try {
                AppContext.get().getApiClient().closeMaintenanceTicket(ticket.getId());
                ticket.setStatus(MaintenanceStatus.CLOSED);
                Platform.runLater(() -> {
                    setLoading(false);
                    changed = true;
                    setTicket(ticket);
                });
            } catch (ApiException e) {
                Platform.runLater(() -> { setLoading(false); showError(e.getMessage()); });
            }
        });
    }

    @FXML
    private void onDone() {
        closeDialog();
    }

    private void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
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
        doneBtn.setDisable(loading);
    }

    private void closeDialog() {
        ((Stage) doneBtn.getScene().getWindow()).close();
    }
}
