package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.client.util.UiUtils;
import com.labhub.common.dto.MaintenanceTicketDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.MaintenancePriority;
import com.labhub.common.enums.MaintenanceStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/** Controller for the Maintenance view. */
public class MaintenanceController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceController.class);
    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> priorityFilter;
    @FXML private Button           newTicketBtn;
    @FXML private HBox             loadingBox;

    @FXML private TableView<MaintenanceTicketDto>              maintenanceTable;
    @FXML private TableColumn<MaintenanceTicketDto, String>   colAsset;
    @FXML private TableColumn<MaintenanceTicketDto, String>   colEquipment;
    @FXML private TableColumn<MaintenanceTicketDto, String>   colPriority;
    @FXML private TableColumn<MaintenanceTicketDto, String>   colStatus;
    @FXML private TableColumn<MaintenanceTicketDto, String>   colAssignee;
    @FXML private TableColumn<MaintenanceTicketDto, String>   colOpened;
    @FXML private TableColumn<MaintenanceTicketDto, String>   colActions;

    @FXML private Label pageInfoLabel;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    private int currentPage = 1;
    private int totalPages  = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        setupTable();
        loadPage(1);
    }

    @FXML private void onFilter() { loadPage(1); }

    @FXML
    private void onNewTicket() {
        UiUtils.showInfo("File Maintenance Ticket",
                "Maintenance ticket form opens here.\nSelect equipment and describe the issue.");
    }

    @FXML private void onRowClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            MaintenanceTicketDto sel = maintenanceTable.getSelectionModel().getSelectedItem();
            if (sel != null) showDetail(sel);
        }
    }

    @FXML private void onPrevPage() { if (currentPage > 1) loadPage(currentPage - 1); }
    @FXML private void onNextPage() { if (currentPage < totalPages) loadPage(currentPage + 1); }

    private void loadPage(int page) {
        setLoading(true);
        Thread.ofVirtual().name("maint-load").start(() -> {
            try {
                PagedResult<MaintenanceTicketDto> result =
                        AppContext.get().getApiClient().getMaintenanceTickets(page, PAGE_SIZE);
                Platform.runLater(() -> { setLoading(false); applyResult(result, page); });
            } catch (ApiException ex) {
                LOG.error("Failed to load maintenance: {}", ex.getMessage());
                Platform.runLater(() -> { setLoading(false); UiUtils.showError("Error", ex.getMessage()); });
            }
        });
    }

    private void applyResult(PagedResult<MaintenanceTicketDto> result, int page) {
        maintenanceTable.getItems().clear();
        if (result.getItems() != null) maintenanceTable.getItems().addAll(result.getItems());
        currentPage = page; totalPages = result.getTotalPages();
        pageInfoLabel.setText("Page " + page + " of " + totalPages
                + "  ·  " + result.getTotalItems() + " tickets");
        prevBtn.setDisable(page <= 1);
        nextBtn.setDisable(page >= totalPages);
    }

    private void setupTable() {
        colAsset.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAssetCode()));
        colEquipment.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEquipmentName()));
        colPriority.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPriority() != null ? c.getValue().getPriority().name() : "—"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() != null ? c.getValue().getStatus().name() : "—"));
        colAssignee.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getAssigneeName() != null ? c.getValue().getAssigneeName() : "Unassigned"));
        colOpened.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getOpenedAt() != null ? c.getValue().getOpenedAt().format(DT_FMT) : "—"));
        colActions.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void showDetail(MaintenanceTicketDto t) {
        UiUtils.showInfo("Maintenance Ticket #" + t.getId(),
                "Equipment:  " + t.getEquipmentName() + " (" + t.getAssetCode() + ")"
                + "\nReporter:   " + t.getReporterName()
                + "\nPriority:   " + (t.getPriority() != null ? t.getPriority().name() : "—")
                + "\nStatus:     " + (t.getStatus() != null ? t.getStatus().name() : "—")
                + "\nAssignee:   " + (t.getAssigneeName() != null ? t.getAssigneeName() : "Unassigned")
                + "\nOpened:     " + (t.getOpenedAt() != null ? t.getOpenedAt().format(DT_FMT) : "—")
                + "\nDescription:\n" + t.getDescription()
                + (t.getResolutionNotes() != null ? "\nResolution:\n" + t.getResolutionNotes() : ""));
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("All Statuses",
                MaintenanceStatus.OPEN.name(), MaintenanceStatus.ASSIGNED.name(),
                MaintenanceStatus.IN_PROGRESS.name(), MaintenanceStatus.RESOLVED.name(),
                MaintenanceStatus.CLOSED.name());
        statusFilter.setValue("All Statuses");
        priorityFilter.getItems().addAll("All Priorities",
                MaintenancePriority.URGENT.name(), MaintenancePriority.HIGH.name(),
                MaintenancePriority.MEDIUM.name(), MaintenancePriority.LOW.name());
        priorityFilter.setValue("All Priorities");
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading); loadingBox.setManaged(loading);
    }
}
