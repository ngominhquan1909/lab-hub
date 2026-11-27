package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.BorrowRequestDto;
import com.labhub.common.enums.EquipmentCondition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for the "Record Item Condition" modal dialog, shared by the checkout
 * (hand-off to borrower) and return (hand-back to the lab) flows for a borrow request.
 * Call {@link #setup(Mode, BorrowRequestDto)} before the dialog is shown.
 */
public class ItemConditionDialogController {

    private static final Logger LOG = LoggerFactory.getLogger(ItemConditionDialogController.class);

    /** Which borrow-request transition this dialog is recording conditions for. */
    public enum Mode { CHECKOUT, RETURN }

    @FXML private Label    titleLabel;
    @FXML private Label    subtitleLabel;
    @FXML private Label    errorLabel;
    @FXML private VBox     itemsBox;
    @FXML private HBox     loadingBox;
    @FXML private Button   saveBtn;

    private final Map<Long, ComboBox<String>> combosByEquipmentId = new LinkedHashMap<>();
    private Mode mode;
    private BorrowRequestDto request;
    private BorrowRequestDto updatedRequest;

    public BorrowRequestDto getUpdatedRequest() {
        return updatedRequest;
    }

    /**
     * Populates the dialog with one condition picker per borrowed item.
     *
     * @param mode    CHECKOUT or RETURN
     * @param request the borrow request whose items need a condition recorded
     */
    public void setup(Mode mode, BorrowRequestDto request) {
        this.mode = mode;
        this.request = request;

        boolean isCheckout = mode == Mode.CHECKOUT;
        titleLabel.setText(isCheckout
                ? "Check Out Equipment — Request #" + request.getId()
                : "Return Equipment — Request #" + request.getId());
        subtitleLabel.setText(isCheckout
                ? "Confirm the condition of each item as it leaves the lab."
                : "Record the condition of each item as it comes back.");
        saveBtn.setText(isCheckout ? "Confirm Checkout" : "Confirm Return");

        itemsBox.getChildren().clear();
        combosByEquipmentId.clear();
        if (request.getItems() != null) {
            for (BorrowRequestDto.BorrowItemDto item : request.getItems()) {
                itemsBox.getChildren().add(buildItemRow(item));
            }
        }
    }

    private HBox buildItemRow(BorrowRequestDto.BorrowItemDto item) {
        Label nameLabel = new Label(item.getEquipmentName() + "  (" + item.getAssetCode() + ")");
        nameLabel.getStyleClass().add("detail-field-value");
        nameLabel.setMaxWidth(260);
        nameLabel.setWrapText(true);

        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(
                EquipmentCondition.EXCELLENT.name(), EquipmentCondition.GOOD.name(),
                EquipmentCondition.FAIR.name(), EquipmentCondition.POOR.name());
        combo.setValue(EquipmentCondition.GOOD.name());
        combo.getStyleClass().add("combo-box");

        combosByEquipmentId.put(item.getEquipmentId(), combo);

        HBox row = new HBox(10, nameLabel, combo);
        row.setStyle("-fx-alignment: CENTER_LEFT;");
        return row;
    }

    @FXML
    private void onSave() {
        hideError();
        Map<Long, EquipmentCondition> conditions = new LinkedHashMap<>();
        combosByEquipmentId.forEach((equipmentId, combo) ->
                conditions.put(equipmentId, EquipmentCondition.valueOf(combo.getValue())));

        setLoading(true);
        Thread.ofVirtual().name("item-condition-save").start(() -> {
            try {
                BorrowRequestDto result = mode == Mode.CHECKOUT
                        ? AppContext.get().getApiClient().checkoutBorrowRequest(request.getId(), conditions)
                        : AppContext.get().getApiClient().returnEquipment(request.getId(), conditions);
                Platform.runLater(() -> {
                    updatedRequest = result;
                    closeDialog();
                });
            } catch (ApiException e) {
                LOG.error("Failed to record item conditions: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onCancel() {
        updatedRequest = null;
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
