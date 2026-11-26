package com.labhub.client.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.EquipmentDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the read-only Equipment Detail dialog, which also shows a
 * combined, newest-first history of borrow loans and maintenance tickets.
 * Call {@link #setEquipment(EquipmentDto)} before the dialog is shown.
 */
public class EquipmentDetailDialogController {

    private static final Logger LOG = LoggerFactory.getLogger(EquipmentDetailDialogController.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML private Label    titleLabel;
    @FXML private Label    assetCodeLabel;
    @FXML private Label    statusLabel;
    @FXML private Label    nameLabel;
    @FXML private Label    conditionLabel;
    @FXML private Label    categoryLabel;
    @FXML private Label    labLabel;
    @FXML private Label    manufacturerLabel;
    @FXML private Label    modelLabel;
    @FXML private Label    serialLabel;
    @FXML private Label    notesLabel;
    @FXML private HBox     historyLoadingBox;
    @FXML private ListView<String> historyList;

    /**
     * Populates the static fields and kicks off the async history load.
     *
     * @param e the equipment to display
     */
    public void setEquipment(EquipmentDto e) {
        titleLabel.setText("Equipment — " + e.getName());
        assetCodeLabel.setText(e.getAssetCode());
        statusLabel.setText(e.getStatus() != null ? e.getStatus().name() : "—");
        nameLabel.setText(e.getName());
        conditionLabel.setText(e.getCondition() != null ? e.getCondition().name() : "—");
        categoryLabel.setText(e.getCategoryName() != null ? e.getCategoryName() : "—");
        labLabel.setText(e.getLabName() != null ? e.getLabName() : "Unassigned");
        manufacturerLabel.setText(e.getManufacturer() != null ? e.getManufacturer() : "—");
        modelLabel.setText(e.getModel() != null ? e.getModel() : "—");
        serialLabel.setText(e.getSerialNumber() != null ? e.getSerialNumber() : "—");
        notesLabel.setText(e.getNotes() != null ? e.getNotes() : "—");

        loadHistory(e.getId());
    }

    private void loadHistory(long equipmentId) {
        setHistoryLoading(true);
        Thread.ofVirtual().name("equip-history").start(() -> {
            try {
                var entries = AppContext.get().getApiClient().getEquipmentHistory(equipmentId);
                Platform.runLater(() -> {
                    setHistoryLoading(false);
                    historyList.getItems().clear();
                    if (entries.isEmpty()) {
                        historyList.getItems().add("No borrow or maintenance history yet.");
                        return;
                    }
                    for (JsonNode entry : entries) {
                        historyList.getItems().add(formatEntry(entry));
                    }
                });
            } catch (ApiException ex) {
                LOG.error("Failed to load equipment history: {}", ex.getMessage());
                Platform.runLater(() -> {
                    setHistoryLoading(false);
                    historyList.getItems().add("Failed to load history: " + ex.getMessage());
                });
            }
        });
    }

    private String formatEntry(JsonNode entry) {
        String type   = textOf(entry, "type");
        String date   = formatDate(textOf(entry, "date"));
        String status = textOf(entry, "status");
        String person = textOf(entry, "personName");
        String summary = textOf(entry, "summary");

        StringBuilder sb = new StringBuilder();
        sb.append(date).append("  ·  [").append(type).append("]  ")
          .append(summary).append("  —  ").append(person)
          .append("  (").append(status).append(")");

        if (entry.has("conditionAtCheckout")) {
            sb.append("\n      Condition at checkout: ").append(entry.get("conditionAtCheckout").asText());
        }
        if (entry.has("conditionAtReturn")) {
            sb.append("  →  at return: ").append(entry.get("conditionAtReturn").asText());
        }
        if (entry.has("resolutionNotes")) {
            sb.append("\n      Resolution: ").append(entry.get("resolutionNotes").asText());
        }
        return sb.toString();
    }

    private String textOf(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "—";
    }

    private String formatDate(String isoLikeText) {
        try {
            return LocalDateTime.parse(isoLikeText).format(DT_FMT);
        } catch (Exception e) {
            return isoLikeText;
        }
    }

    @FXML
    private void onClose() {
        ((Stage) titleLabel.getScene().getWindow()).close();
    }

    private void setHistoryLoading(boolean loading) {
        historyLoadingBox.setVisible(loading);
        historyLoadingBox.setManaged(loading);
    }
}
