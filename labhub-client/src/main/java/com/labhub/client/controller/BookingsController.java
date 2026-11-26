package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiClient;
import com.labhub.client.network.ApiException;
import com.labhub.client.util.UiUtils;
import com.labhub.common.dto.BookingDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.BookingStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Bookings view.
 * Displays bookings with inline approve/reject actions for managers.
 */
public class BookingsController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(BookingsController.class);
    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> labFilter;
    @FXML private Button           newBookingBtn;
    @FXML private HBox             loadingBox;

    @FXML private TableView<BookingDto>              bookingsTable;
    @FXML private TableColumn<BookingDto, String>   colLab;
    @FXML private TableColumn<BookingDto, String>   colDate;
    @FXML private TableColumn<BookingDto, String>   colTime;
    @FXML private TableColumn<BookingDto, String>   colPurpose;
    @FXML private TableColumn<BookingDto, String>   colRequester;
    @FXML private TableColumn<BookingDto, String>   colStatus;
    @FXML private TableColumn<BookingDto, String>   colActions;

    @FXML private Label pageInfoLabel;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    private int currentPage = 1;
    private int totalPages  = 1;
    private final boolean isManager = AppContext.get().isManager();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        setupTable();
        loadPage(1);
    }

    // ── Event handlers ────────────────────────────────────────────────────

    @FXML
    private void onFilter() { loadPage(1); }

    @FXML
    private void onNewBooking() {
        UiUtils.showInfo("New Booking",
                "Booking creation dialog opens here.\n"
                + "Implement NewBookingDialog.fxml for the full flow.");
    }

    @FXML
    private void onRowClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            BookingDto selected = bookingsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showBookingDetail(selected);
            }
        }
    }

    @FXML
    private void onPrevPage() { if (currentPage > 1) loadPage(currentPage - 1); }

    @FXML
    private void onNextPage() { if (currentPage < totalPages) loadPage(currentPage + 1); }

    // ── Data loading ──────────────────────────────────────────────────────

    private void loadPage(int page) {
        setLoading(true);
        BookingStatus status = null;
        String sv = statusFilter.getValue();
        if (sv != null && !sv.equals("All Statuses")) {
            try { status = BookingStatus.valueOf(sv); }
            catch (IllegalArgumentException ignored) { }
        }
        final BookingStatus finalStatus = status;

        Thread.ofVirtual().name("bookings-load").start(() -> {
            try {
                PagedResult<BookingDto> result =
                        AppContext.get().getApiClient().getBookings(finalStatus, null, page, PAGE_SIZE);
                Platform.runLater(() -> {
                    setLoading(false);
                    applyResult(result, page);
                });
            } catch (ApiException e) {
                LOG.error("Failed to load bookings: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    UiUtils.showError("Error", e.getMessage());
                });
            }
        });
    }

    private void applyResult(PagedResult<BookingDto> result, int page) {
        bookingsTable.getItems().clear();
        if (result.getItems() != null) {
            bookingsTable.getItems().addAll(result.getItems());
        }
        currentPage = page;
        totalPages  = result.getTotalPages();
        pageInfoLabel.setText("Page " + page + " of " + totalPages
                + "  ·  " + result.getTotalItems() + " bookings");
        prevBtn.setDisable(page <= 1);
        nextBtn.setDisable(page >= totalPages);
    }

    // ── Table setup ───────────────────────────────────────────────────────

    private void setupTable() {
        colLab.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getLabName() + "\n"
                        + (c.getValue().getLabCode() != null ? c.getValue().getLabCode() : "")));
        colDate.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStartTime() != null
                        ? c.getValue().getStartTime().format(DATE_FMT) : "—"));
        colTime.setCellValueFactory(c -> {
            BookingDto b = c.getValue();
            if (b.getStartTime() == null || b.getEndTime() == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(
                    b.getStartTime().format(TIME_FMT) + " – " + b.getEndTime().format(TIME_FMT));
        });
        colPurpose.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getPurpose()));
        colRequester.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRequesterName()));
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus() != null
                        ? c.getValue().getStatus().name() : "—"));

        // Actions column — approve/reject for managers, cancel for owners
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn  = new Button("Reject");
            private final HBox   box        = new HBox(4, approveBtn, rejectBtn);

            {
                approveBtn.getStyleClass().add("btn-success");
                approveBtn.setStyle("-fx-font-size:11px; -fx-padding:4 8;");
                rejectBtn.getStyleClass().add("btn-danger");
                rejectBtn.setStyle("-fx-font-size:11px; -fx-padding:4 8;");
                approveBtn.setOnAction(e -> handleApprove(getItem() != null
                        ? getTableView().getItems().get(getIndex()) : null));
                rejectBtn.setOnAction(e -> handleReject(getItem() != null
                        ? getTableView().getItems().get(getIndex()) : null));
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                BookingDto booking = getTableView().getItems().get(getIndex());
                boolean isPending = booking.getStatus() == BookingStatus.PENDING;
                boolean showActions = isManager && isPending;
                approveBtn.setVisible(showActions);
                approveBtn.setManaged(showActions);
                rejectBtn.setVisible(showActions);
                rejectBtn.setManaged(showActions);
                setGraphic(box);
            }
        });
    }

    private void handleApprove(BookingDto booking) {
        if (booking == null) return;
        boolean confirmed = UiUtils.showConfirm("Approve Booking",
                "Approve booking for " + booking.getLabName()
                + " by " + booking.getRequesterName() + "?");
        if (!confirmed) return;

        setLoading(true);
        Thread.ofVirtual().name("booking-approve").start(() -> {
            try {
                AppContext.get().getApiClient().approveBooking(booking.getId(), null);
                Platform.runLater(() -> { setLoading(false); loadPage(currentPage); });
            } catch (ApiException e) {
                Platform.runLater(() -> { setLoading(false); UiUtils.showError("Error", e.getMessage()); });
            }
        });
    }

    private void handleReject(BookingDto booking) {
        if (booking == null) return;
        Optional<String> note = UiUtils.showTextInput("Reject Booking",
                "Provide a reason for rejecting this booking:", "");
        if (note.isEmpty() || note.get().isBlank()) return;

        setLoading(true);
        Thread.ofVirtual().name("booking-reject").start(() -> {
            try {
                AppContext.get().getApiClient().rejectBooking(booking.getId(), note.get());
                Platform.runLater(() -> { setLoading(false); loadPage(currentPage); });
            } catch (ApiException e) {
                Platform.runLater(() -> { setLoading(false); UiUtils.showError("Error", e.getMessage()); });
            }
        });
    }

    private void showBookingDetail(BookingDto b) {
        UiUtils.showInfo("Booking Detail",
                "Lab:       " + b.getLabName()
                + "\nDate:      " + (b.getStartTime() != null ? b.getStartTime().format(DATE_FMT) : "—")
                + "\nTime:      " + (b.getStartTime() != null
                        ? b.getStartTime().format(TIME_FMT) + " – " + b.getEndTime().format(TIME_FMT) : "—")
                + "\nPurpose:   " + b.getPurpose()
                + "\nAttendees: " + (b.getAttendeeCount() != null ? b.getAttendeeCount() : "—")
                + "\nRequester: " + b.getRequesterName()
                + "\nStatus:    " + (b.getStatus() != null ? b.getStatus().name() : "—")
                + (b.getDecisionNote() != null ? "\nNote:      " + b.getDecisionNote() : ""));
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("All Statuses",
                BookingStatus.PENDING.name(), BookingStatus.APPROVED.name(),
                BookingStatus.REJECTED.name(), BookingStatus.CANCELLED.name());
        statusFilter.setValue("All Statuses");
        labFilter.getItems().add("All Labs");
        labFilter.setValue("All Labs");
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
    }
}
