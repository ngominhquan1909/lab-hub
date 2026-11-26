package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.client.util.UiUtils;
import com.labhub.common.dto.BorrowRequestDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.BorrowStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
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
 * Controller for the Borrow Requests view.
 */
public class BorrowController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(BorrowController.class);
    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML private ComboBox<String> statusFilter;
    @FXML private Button           newRequestBtn;
    @FXML private HBox             loadingBox;

    @FXML private TableView<BorrowRequestDto>              borrowTable;
    @FXML private TableColumn<BorrowRequestDto, String>   colId;
    @FXML private TableColumn<BorrowRequestDto, String>   colRequester;
    @FXML private TableColumn<BorrowRequestDto, String>   colPurpose;
    @FXML private TableColumn<BorrowRequestDto, String>   colItems;
    @FXML private TableColumn<BorrowRequestDto, String>   colDueDate;
    @FXML private TableColumn<BorrowRequestDto, String>   colStatus;
    @FXML private TableColumn<BorrowRequestDto, String>   colActions;

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

    @FXML private void onFilter() { loadPage(1); }

    @FXML
    private void onNewRequest() {
        Object controller = com.labhub.client.app.ViewManager.get()
                .showDialog("NewBorrowDialog.fxml", "New Borrow Request");
        if (controller instanceof NewBorrowDialogController dialog
                && dialog.getCreatedRequest() != null) {
            loadPage(1);
        }
    }

    @FXML private void onRowClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            BorrowRequestDto sel = borrowTable.getSelectionModel().getSelectedItem();
            if (sel != null) showDetail(sel);
        }
    }

    @FXML private void onPrevPage() { if (currentPage > 1) loadPage(currentPage - 1); }
    @FXML private void onNextPage() { if (currentPage < totalPages) loadPage(currentPage + 1); }

    private void loadPage(int page) {
        setLoading(true);
        BorrowStatus status = null;
        String sv = statusFilter.getValue();
        if (sv != null && !sv.equals("All Statuses")) {
            try { status = BorrowStatus.valueOf(sv); } catch (IllegalArgumentException ignored) { }
        }
        final BorrowStatus fs = status;
        Thread.ofVirtual().name("borrow-load").start(() -> {
            try {
                PagedResult<BorrowRequestDto> result =
                        AppContext.get().getApiClient().getBorrowRequests(fs, page, PAGE_SIZE);
                Platform.runLater(() -> { setLoading(false); applyResult(result, page); });
            } catch (ApiException ex) {
                LOG.error("Failed to load borrows: {}", ex.getMessage());
                Platform.runLater(() -> { setLoading(false); UiUtils.showError("Error", ex.getMessage()); });
            }
        });
    }

    private void applyResult(PagedResult<BorrowRequestDto> result, int page) {
        borrowTable.getItems().clear();
        if (result.getItems() != null) borrowTable.getItems().addAll(result.getItems());
        currentPage = page; totalPages = result.getTotalPages();
        pageInfoLabel.setText("Page " + page + " of " + totalPages
                + "  ·  " + result.getTotalItems() + " requests");
        prevBtn.setDisable(page <= 1);
        nextBtn.setDisable(page >= totalPages);
    }

    private void setupTable() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getId()));
        colRequester.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRequesterName()));
        colPurpose.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPurpose()));
        colItems.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getItems() != null ? String.valueOf(c.getValue().getItems().size()) : "—"));
        colDueDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getExpectedReturnAt() != null
                        ? c.getValue().getExpectedReturnAt().format(DT_FMT) : "—"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() != null ? c.getValue().getStatus().name() : "—"));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn  = smallButton("Approve",  "btn-success");
            private final Button rejectBtn   = smallButton("Reject",   "btn-danger");
            private final Button checkoutBtn = smallButton("Check Out","btn-primary");
            private final Button returnBtn   = smallButton("Return",   "btn-success");
            private final Button cancelBtn   = smallButton("Cancel",   "btn-secondary");
            private final HBox   box = new HBox(4, approveBtn, rejectBtn, checkoutBtn, returnBtn, cancelBtn);
            {
                approveBtn.setOnAction(e -> handleApprove(rowItem()));
                rejectBtn.setOnAction(e -> handleReject(rowItem()));
                checkoutBtn.setOnAction(e -> handleCheckout(rowItem()));
                returnBtn.setOnAction(e -> handleReturn(rowItem()));
                cancelBtn.setOnAction(e -> handleCancel(rowItem()));
            }

            private BorrowRequestDto rowItem() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                BorrowRequestDto req = rowItem();
                BorrowStatus status = req.getStatus();
                boolean isOwner = req.getRequesterId() != null
                        && req.getRequesterId().equals(AppContext.get().getCurrentUser().getId());

                show(approveBtn,  isManager && status == BorrowStatus.PENDING);
                show(rejectBtn,   isManager && status == BorrowStatus.PENDING);
                show(checkoutBtn, isManager && status == BorrowStatus.APPROVED);
                show(returnBtn,   isManager && (status == BorrowStatus.ACTIVE || status == BorrowStatus.OVERDUE));
                show(cancelBtn,   (isManager || isOwner)
                        && (status == BorrowStatus.PENDING || status == BorrowStatus.APPROVED));
                setGraphic(box);
            }

            private void show(Button b, boolean visible) {
                b.setVisible(visible);
                b.setManaged(visible);
            }
        });
    }

    private void handleApprove(BorrowRequestDto req) {
        boolean ok = UiUtils.showConfirm("Approve Borrow Request",
                "Approve borrow request #" + req.getId() + " from " + req.getRequesterName() + "?");
        if (!ok) return;
        Thread.ofVirtual().name("borrow-approve").start(() -> {
            try {
                AppContext.get().getApiClient().approveBorrowRequest(req.getId(), null);
                Platform.runLater(() -> loadPage(currentPage));
            } catch (ApiException ex) {
                Platform.runLater(() -> UiUtils.showError("Error", ex.getMessage()));
            }
        });
    }

    private void handleReject(BorrowRequestDto req) {
        Optional<String> note = UiUtils.showTextInput("Reject Borrow Request",
                "Reason for rejection:", "");
        if (note.isEmpty() || note.get().isBlank()) return;
        Thread.ofVirtual().name("borrow-reject").start(() -> {
            try {
                AppContext.get().getApiClient().rejectBorrowRequest(req.getId(), note.get());
                Platform.runLater(() -> loadPage(currentPage));
            } catch (ApiException ex) {
                Platform.runLater(() -> UiUtils.showError("Error", ex.getMessage()));
            }
        });
    }

    private void showDetail(BorrowRequestDto r) {
        StringBuilder items = new StringBuilder();
        if (r.getItems() != null) {
            for (BorrowRequestDto.BorrowItemDto item : r.getItems()) {
                items.append("\n   • ").append(item.getEquipmentName())
                        .append(" (").append(item.getAssetCode()).append(")");
                if (item.getConditionAtCheckout() != null) {
                    items.append("  [checkout: ").append(item.getConditionAtCheckout()).append("]");
                }
                if (item.getConditionAtReturn() != null) {
                    items.append("  [return: ").append(item.getConditionAtReturn()).append("]");
                }
            }
        }
        UiUtils.showInfo("Borrow Request #" + r.getId(),
                "Requester:   " + r.getRequesterName()
                + "\nPurpose:     " + r.getPurpose()
                + "\nDue:         " + (r.getExpectedReturnAt() != null
                        ? r.getExpectedReturnAt().format(DT_FMT) : "—")
                + "\nStatus:      " + (r.getStatus() != null ? r.getStatus().name() : "—")
                + "\nItems:" + (items.length() > 0 ? items : "  (none)"));
    }

    private void handleCheckout(BorrowRequestDto req) {
        Object controller = com.labhub.client.app.ViewManager.get().showDialog(
                "ItemConditionDialog.fxml", "Check Out Equipment",
                c -> ((ItemConditionDialogController) c)
                        .setup(ItemConditionDialogController.Mode.CHECKOUT, req));
        if (controller instanceof ItemConditionDialogController dialog
                && dialog.getUpdatedRequest() != null) {
            loadPage(currentPage);
        }
    }

    private void handleReturn(BorrowRequestDto req) {
        Object controller = com.labhub.client.app.ViewManager.get().showDialog(
                "ItemConditionDialog.fxml", "Return Equipment",
                c -> ((ItemConditionDialogController) c)
                        .setup(ItemConditionDialogController.Mode.RETURN, req));
        if (controller instanceof ItemConditionDialogController dialog
                && dialog.getUpdatedRequest() != null) {
            loadPage(currentPage);
        }
    }

    private void handleCancel(BorrowRequestDto req) {
        boolean ok = UiUtils.showConfirm("Cancel Borrow Request",
                "Cancel borrow request #" + req.getId() + "?");
        if (!ok) return;
        Thread.ofVirtual().name("borrow-cancel").start(() -> {
            try {
                AppContext.get().getApiClient().cancelBorrowRequest(req.getId());
                Platform.runLater(() -> loadPage(currentPage));
            } catch (ApiException ex) {
                Platform.runLater(() -> UiUtils.showError("Error", ex.getMessage()));
            }
        });
    }

    private Button smallButton(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        b.setStyle("-fx-font-size:11px; -fx-padding:4 8;");
        return b;
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("All Statuses",
                BorrowStatus.PENDING.name(), BorrowStatus.APPROVED.name(),
                BorrowStatus.ACTIVE.name(), BorrowStatus.OVERDUE.name(),
                BorrowStatus.RETURNED.name(), BorrowStatus.REJECTED.name());
        statusFilter.setValue("All Statuses");
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
    }
}
