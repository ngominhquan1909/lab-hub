package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiClient;
import com.labhub.client.network.ApiException;
import com.labhub.client.util.UiUtils;
import com.labhub.common.dto.BookingDto;
import com.labhub.common.dto.DashboardStatsDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.BookingStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for the Dashboard view.
 * Loads stats and today's bookings on a background thread.
 */
public class DashboardController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM");

    // Stat labels
    @FXML private Label totalLabsValue;
    @FXML private Label totalEquipValue;
    @FXML private Label availableEquipValue;
    @FXML private Label maintenanceValue;
    @FXML private Label overdueValue;
    @FXML private Label pendingBookingsValue;
    @FXML private Label pendingBorrowsValue;
    @FXML private Label openMaintenanceValue;
    @FXML private Label todayBookingsLabel;
    @FXML private HBox  loadingBox;

    // Today's schedule table
    @FXML private TableView<BookingDto>              todayTable;
    @FXML private TableColumn<BookingDto, String>   todayLabCol;
    @FXML private TableColumn<BookingDto, String>   todayTimeCol;
    @FXML private TableColumn<BookingDto, String>   todayPurposeCol;
    @FXML private TableColumn<BookingDto, String>   todayRequesterCol;
    @FXML private TableColumn<BookingDto, String>   todayStatusCol;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadData();
    }

    // ── Event handlers ────────────────────────────────────────────────────

    @FXML
    private void onRefresh() {
        loadData();
    }

    @FXML
    private void onGoToBookings() {
        // Delegate navigation up — this is a simplification; a proper event bus would be better
        LOG.debug("Navigate to bookings (triggered from dashboard).");
    }

    @FXML
    private void onGoToBorrow() {
        LOG.debug("Navigate to borrow (triggered from dashboard).");
    }

    @FXML
    private void onGoToMaintenance() {
        LOG.debug("Navigate to maintenance (triggered from dashboard).");
    }

    // ── Data loading ──────────────────────────────────────────────────────

    private void loadData() {
        setLoading(true);
        Thread.ofVirtual().name("dashboard-load").start(() -> {
            try {
                ApiClient api = AppContext.get().getApiClient();
                DashboardStatsDto stats = api.getDashboardStats();
                PagedResult<BookingDto> todayBookings =
                        api.getBookings(BookingStatus.APPROVED, null, 1, 50);

                Platform.runLater(() -> {
                    setLoading(false);
                    applyStats(stats);
                    applyTodayBookings(todayBookings);
                });
            } catch (ApiException e) {
                LOG.error("Failed to load dashboard: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    UiUtils.showError("Dashboard Error",
                            "Could not load dashboard data: " + e.getMessage());
                });
            }
        });
    }

    private void applyStats(DashboardStatsDto stats) {
        totalLabsValue.setText(String.valueOf(stats.getTotalActiveLabs()));
        totalEquipValue.setText(String.valueOf(stats.getTotalEquipment()));
        availableEquipValue.setText(String.valueOf(stats.getAvailableEquipment()));
        maintenanceValue.setText(String.valueOf(stats.getMaintenanceEquipment()));
        overdueValue.setText(String.valueOf(stats.getOverdueItems()));
        pendingBookingsValue.setText(String.valueOf(stats.getPendingBookings()));
        pendingBorrowsValue.setText(String.valueOf(stats.getPendingBorrowRequests()));
        openMaintenanceValue.setText(String.valueOf(stats.getOpenMaintenanceTickets()));
        todayBookingsLabel.setText(stats.getTodayBookings() + " booking(s) today");
    }

    private void applyTodayBookings(PagedResult<BookingDto> result) {
        todayTable.getItems().clear();
        if (result != null && result.getItems() != null) {
            todayTable.getItems().addAll(result.getItems());
        }
    }

    // ── Table setup ───────────────────────────────────────────────────────

    private void setupTable() {
        todayLabCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getLabName()));
        todayTimeCol.setCellValueFactory(c -> {
            BookingDto b = c.getValue();
            if (b.getStartTime() == null || b.getEndTime() == null) {
                return new SimpleStringProperty("—");
            }
            String s = b.getStartTime().format(TIME_FMT) + " – " + b.getEndTime().format(TIME_FMT);
            return new SimpleStringProperty(s);
        });
        todayPurposeCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPurpose()));
        todayRequesterCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRequesterName()));
        todayStatusCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus() != null
                        ? c.getValue().getStatus().name() : "—"));
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
    }
}
