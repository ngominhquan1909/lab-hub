package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.BookingDto;
import com.labhub.common.dto.LabDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller for the "New Booking" modal dialog.
 * On success, {@link #getCreatedBooking()} returns the new booking so the caller can refresh its table.
 */
public class NewBookingDialogController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(NewBookingDialogController.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label           errorLabel;
    @FXML private ComboBox<String> labCombo;
    @FXML private DatePicker      datePicker;
    @FXML private TextField       startTimeField;
    @FXML private TextField       endTimeField;
    @FXML private TextArea        purposeField;
    @FXML private TextField       attendeesField;
    @FXML private HBox            loadingBox;
    @FXML private Button          saveBtn;

    private final Map<String, Long> labIdsByName = new LinkedHashMap<>();
    private BookingDto createdBooking;

    /** @return the created booking, or null if cancelled or save failed. */
    public BookingDto getCreatedBooking() {
        return createdBooking;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        datePicker.setValue(LocalDate.now().plusDays(1));
        Thread.ofVirtual().name("new-booking-labs").start(() -> {
            try {
                var labs = AppContext.get().getApiClient().getLabs(null, 1, 200);
                Platform.runLater(() -> {
                    if (labs.getItems() != null) {
                        for (LabDto l : labs.getItems()) {
                            labIdsByName.put(l.getName(), l.getId());
                            labCombo.getItems().add(l.getName());
                        }
                        if (!labCombo.getItems().isEmpty()) {
                            labCombo.setValue(labCombo.getItems().get(0));
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

        String labName = labCombo.getValue();
        LocalDate date = datePicker.getValue();
        String purpose = purposeField.getText() == null ? "" : purposeField.getText().trim();

        if (labName == null || date == null || purpose.isEmpty()) {
            showError("Laboratory, date, and purpose are all required.");
            return;
        }

        LocalTime startTime;
        LocalTime endTime;
        try {
            startTime = LocalTime.parse(startTimeField.getText().trim(), TIME_FMT);
            endTime   = LocalTime.parse(endTimeField.getText().trim(), TIME_FMT);
        } catch (Exception e) {
            showError("Start and end time must be in HH:mm format, e.g. 09:00.");
            return;
        }
        if (!endTime.isAfter(startTime)) {
            showError("End time must be after start time.");
            return;
        }

        Integer attendees = null;
        if (attendeesField.getText() != null && !attendeesField.getText().isBlank()) {
            try {
                attendees = Integer.parseInt(attendeesField.getText().trim());
            } catch (NumberFormatException ignored) { }
        }

        BookingDto dto = new BookingDto();
        dto.setLabId(labIdsByName.get(labName));
        dto.setStartTime(LocalDateTime.of(date, startTime));
        dto.setEndTime(LocalDateTime.of(date, endTime));
        dto.setPurpose(purpose);
        dto.setAttendeeCount(attendees);

        setLoading(true);
        Thread.ofVirtual().name("new-booking-save").start(() -> {
            try {
                BookingDto created = AppContext.get().getApiClient().createBooking(dto);
                Platform.runLater(() -> {
                    createdBooking = created;
                    closeDialog();
                });
            } catch (ApiException e) {
                LOG.error("Failed to create booking: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
            }
        });
    }

    @FXML
    private void onCancel() {
        createdBooking = null;
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
