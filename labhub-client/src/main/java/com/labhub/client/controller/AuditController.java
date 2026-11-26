package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.client.util.UiUtils;
import com.labhub.common.dto.AuditLogDto;
import com.labhub.common.dto.PagedResult;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/** Controller for the Audit Log view. */
public class AuditController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AuditController.class);
    private static final int PAGE_SIZE = 50;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");

    @FXML private TextField searchField;
    @FXML private HBox      loadingBox;

    @FXML private TableView<AuditLogDto>              auditTable;
    @FXML private TableColumn<AuditLogDto, String>   colTimestamp;
    @FXML private TableColumn<AuditLogDto, String>   colActor;
    @FXML private TableColumn<AuditLogDto, String>   colAction;
    @FXML private TableColumn<AuditLogDto, String>   colTarget;
    @FXML private TableColumn<AuditLogDto, String>   colDetails;

    @FXML private Label  pageInfoLabel;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    private int currentPage = 1;
    private int totalPages  = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadPage(1);
    }

    @FXML private void onSearch() { loadPage(1); }
    @FXML private void onPrevPage() { if (currentPage > 1) loadPage(currentPage - 1); }
    @FXML private void onNextPage() { if (currentPage < totalPages) loadPage(currentPage + 1); }

    private void loadPage(int page) {
        setLoading(true);
        Thread.ofVirtual().name("audit-load").start(() -> {
            try {
                // Re-use AUDIT_LIST via dashboard endpoint
                // The ApiClient doesn't expose a dedicated audit method yet;
                // using a workaround via raw request would be needed in a full implementation.
                // For now we simulate with an empty result until the endpoint is wired.
                PagedResult<AuditLogDto> result = new PagedResult<>(
                        java.util.List.of(), page, PAGE_SIZE, 0);
                Platform.runLater(() -> { setLoading(false); applyResult(result, page); });
            } catch (Exception ex) {
                LOG.error("Failed to load audit log: {}", ex.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    UiUtils.showError("Error", ex.getMessage());
                });
            }
        });
    }

    private void applyResult(PagedResult<AuditLogDto> result, int page) {
        auditTable.getItems().clear();
        if (result.getItems() != null) auditTable.getItems().addAll(result.getItems());
        currentPage = page;
        totalPages  = Math.max(1, result.getTotalPages());
        pageInfoLabel.setText("Page " + page + " of " + totalPages
                + "  ·  " + result.getTotalItems() + " entries");
        prevBtn.setDisable(page <= 1);
        nextBtn.setDisable(page >= totalPages);
    }

    private void setupTable() {
        colTimestamp.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTimestamp() != null
                        ? c.getValue().getTimestamp().format(DT_FMT) : "—"));
        colActor.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getActorName() != null ? c.getValue().getActorName() : "System"));
        colAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        colTarget.setCellValueFactory(c -> {
            String type = c.getValue().getTargetType();
            Long   id   = c.getValue().getTargetId();
            if (type == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(id != null ? type + " #" + id : type);
        });
        colDetails.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDetails() != null ? c.getValue().getDetails() : ""));
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
    }
}
