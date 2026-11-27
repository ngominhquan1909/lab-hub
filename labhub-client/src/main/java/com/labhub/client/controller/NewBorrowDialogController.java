package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.BorrowRequestDto;
import com.labhub.common.dto.EquipmentDto;
import com.labhub.common.enums.EquipmentStatus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the "New Borrow Request" modal dialog.
 * On success, {@link #getCreatedRequest()} returns the new request so the caller can refresh its table.
 */
public class NewBorrowDialogController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(NewBorrowDialogController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label        errorLabel;
    @FXML private ListView<String> equipmentList;
    @FXML private TextArea     purposeField;
    @FXML private DatePicker   returnDatePicker;
    @FXML private TextField    returnTimeField;
    @FXML private HBox         loadingBox;
    @FXML private Button       saveBtn;

    private final Map<String, Long> equipmentIdsByLabel = new LinkedHashMap<>();
    private BorrowRequestDto createdRequest;

    public BorrowRequestDto getCreatedRequest() {
        return createdRequest;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        equipmentList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        returnDatePicker.setValue(LocalDate.now().plusDays(7));
        returnTimeField.setText("17:00");
        loadAvailableEquipment();
    }

    private void loadAvailableEquipment() {
        Thread.ofVirtual().name("new-borrow-equip").start(() -> {
            try {
                var equipment = AppContext.get().getApiClient().getEquipment(null, null, 1, 200);
                Platform.runLater(() -> {
                    if (equipment.getItems() != null) {
                        for (EquipmentDto e : equipment.getItems()) {
                            if (!e.isActive() || e.getStatus() != EquipmentStatus.AVAILABLE) {
                                continue;
                            }
                            String label = e.getName() + "  (" + e.getAssetCode() + ")";
                            equipmentIdsByLabel.put(label, e.getId());
                            equipmentList.getItems().add(label);
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

        List<String> selectedLabels = equipmentList.getSelectionModel().getSelectedItems();
        String purpose = purposeField.getText() == null ? "" : purposeField.getText().trim();
        LocalDate returnDate = returnDatePicker.getValue();

        if (selectedLabels.isEmpty()) {
            showError("Select at least one piece of equipment.");
            return;
        }
        if (purpose.isEmpty()) {
            showError("Purpose is required.");
            return;
        }
        if (returnDate == null) {
            showError("Expected return date is required.");
            return;
        }

        LocalTime returnTime;
        try {
            String timeText = returnTimeField.getText() == null ? "" : returnTimeField.getText().trim();
            returnTime = timeText.isEmpty() ? LocalTime.of(17, 0) : LocalTime.parse(timeText, TIME_FMT);
        } catch (Exception e) {
            showError("Return time must be in HH:mm format, e.g. 17:00.");
            return;
        }

        LocalDateTime expectedReturnAt = LocalDateTime.of(returnDate, returnTime);
        if (!expectedReturnAt.isAfter(LocalDateTime.now())) {
            showError("Expected return date must be in the future.");
            return;
        }

        List<Long> equipmentIds = new ArrayList<>();
        for (String label : selectedLabels) {
            Long id = equipmentIdsByLabel.get(label);
            if (id != null) {
                equipmentIds.add(id);
            }
        }

        BorrowRequestDto dto = new BorrowRequestDto();
        dto.setPurpose(purpose);
        dto.setExpectedReturnAt(expectedReturnAt);

        setLoading(true);
        Thread.ofVirtual().name("new-borrow-save").start(() -> {
            try {
                BorrowRequestDto created = AppContext.get().getApiClient()
                        .createBorrowRequest(dto, equipmentIds);
                Platform.runLater(() -> {
                    createdRequest = created;
                    closeDialog();
                });
            } catch (ApiException e) {
                LOG.error("Failed to create borrow request: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onCancel() {
        createdRequest = null;
        closeDialog();
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
