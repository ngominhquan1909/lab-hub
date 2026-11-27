package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.CategoryDto;
import com.labhub.common.dto.EquipmentDto;
import com.labhub.common.dto.LabDto;
import com.labhub.common.enums.EquipmentCondition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the "Add Equipment" modal dialog.
 * On success, {@link #getCreatedEquipment()} returns the new record so the caller can refresh its table.
 */
public class AddEquipmentDialogController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AddEquipmentDialogController.class);
    private static final String NONE_LABEL = "— Unassigned —";

    @FXML private Label     errorLabel;
    @FXML private TextField assetCodeField;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> labCombo;
    @FXML private TextField manufacturerField;
    @FXML private TextField modelField;
    @FXML private TextField serialNumberField;
    @FXML private ComboBox<String> conditionCombo;
    @FXML private TextArea  notesField;
    @FXML private HBox      loadingBox;
    @FXML private Button    saveBtn;

    private final Map<String, Long> categoryIdsByName = new LinkedHashMap<>();
    private final Map<String, Long> labIdsByName = new LinkedHashMap<>();

    private EquipmentDto createdEquipment;

    public EquipmentDto getCreatedEquipment() {
        return createdEquipment;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        conditionCombo.getItems().addAll(
                EquipmentCondition.EXCELLENT.name(), EquipmentCondition.GOOD.name(),
                EquipmentCondition.FAIR.name(), EquipmentCondition.POOR.name());
        conditionCombo.setValue(EquipmentCondition.GOOD.name());

        labCombo.getItems().add(NONE_LABEL);
        labCombo.setValue(NONE_LABEL);

        loadCategories();
        loadLabs();
    }

    private void loadCategories() {
        Thread.ofVirtual().name("add-equip-categories").start(() -> {
            try {
                var categories = AppContext.get().getApiClient().getCategories();
                Platform.runLater(() -> {
                    for (CategoryDto c : categories) {
                        categoryIdsByName.put(c.getName(), c.getId());
                        categoryCombo.getItems().add(c.getName());
                    }
                });
            } catch (ApiException e) {
                LOG.warn("Failed to load categories: {}", e.getMessage());
            }
        });
    }

    private void loadLabs() {
        Thread.ofVirtual().name("add-equip-labs").start(() -> {
            try {
                var labs = AppContext.get().getApiClient().getLabs(null, 1, 200);
                Platform.runLater(() -> {
                    if (labs.getItems() != null) {
                        for (LabDto l : labs.getItems()) {
                            labIdsByName.put(l.getName(), l.getId());
                            labCombo.getItems().add(l.getName());
                        }
                    }
                });
            } catch (ApiException e) {
                LOG.warn("Failed to load labs: {}", e.getMessage());
            }
        });
    }

    @FXML
    private void onSave() {
        hideError();
        String assetCode = textOf(assetCodeField);
        String name = textOf(nameField);
        if (assetCode.isEmpty() || name.isEmpty()) {
            showError("Asset Code and Name are required.");
            return;
        }

        EquipmentDto dto = new EquipmentDto();
        dto.setAssetCode(assetCode);
        dto.setName(name);
        dto.setManufacturer(blankToNull(manufacturerField.getText()));
        dto.setModel(blankToNull(modelField.getText()));
        dto.setSerialNumber(blankToNull(serialNumberField.getText()));
        dto.setNotes(blankToNull(notesField.getText()));
        dto.setActive(true);

        String categoryName = categoryCombo.getValue();
        if (categoryName != null) {
            dto.setCategoryId(categoryIdsByName.get(categoryName));
        }
        String labName = labCombo.getValue();
        if (labName != null && !labName.equals(NONE_LABEL)) {
            dto.setLabId(labIdsByName.get(labName));
        }
        String conditionName = conditionCombo.getValue();
        if (conditionName != null) {
            dto.setCondition(EquipmentCondition.valueOf(conditionName));
        }

        setLoading(true);
        Thread.ofVirtual().name("add-equipment").start(() -> {
            try {
                EquipmentDto created = AppContext.get().getApiClient().createEquipment(dto);
                Platform.runLater(() -> {
                    createdEquipment = created;
                    closeDialog();
                });
            } catch (ApiException e) {
                LOG.error("Failed to create equipment: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onCancel() {
        createdEquipment = null;
        closeDialog();
    }

    private String textOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
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
