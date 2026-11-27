package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiClient;
import com.labhub.client.network.ApiException;
import com.labhub.client.util.UiUtils;
import com.labhub.common.dto.EquipmentDto;
import com.labhub.common.dto.LabDto;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.enums.EquipmentStatus;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Equipment management view.
 * Provides a searchable, filterable, paginated table of equipment records.
 */
public class EquipmentController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(EquipmentController.class);
    private static final int PAGE_SIZE = 25;

    @FXML private TextField  searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> labFilter;
    @FXML private Button     addButton;
    @FXML private HBox       loadingBox;

    @FXML private TableView<EquipmentDto>              equipmentTable;
    @FXML private TableColumn<EquipmentDto, String>    colAssetCode;
    @FXML private TableColumn<EquipmentDto, String>    colName;
    @FXML private TableColumn<EquipmentDto, String>    colCategory;
    @FXML private TableColumn<EquipmentDto, String>    colLab;
    @FXML private TableColumn<EquipmentDto, String>    colCondition;
    @FXML private TableColumn<EquipmentDto, String>    colStatus;
    @FXML private TableColumn<EquipmentDto, String>    colActions;

    @FXML private Label pageInfoLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;

    private int currentPage = 1;
    private int totalPages  = 1;
    private Long selectedLabId = null;
    private final java.util.Map<String, Long> labIdsByName = new java.util.LinkedHashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupFilters();

        // Hide add button for students
        if (!AppContext.get().isManager()) {
            addButton.setVisible(false);
            addButton.setManaged(false);
        }

        loadPage(1);
    }

    @FXML
    private void onSearch() {
        currentPage = 1;
        loadPage(1);
    }

    @FXML
    private void onAdd() {
        Object controller = com.labhub.client.app.ViewManager.get()
                .showDialog("AddEquipmentDialog.fxml", "Add Equipment");
        if (controller instanceof AddEquipmentDialogController dialog
                && dialog.getCreatedEquipment() != null) {
            loadPage(1);
        }
    }

    @FXML
    private void onRowSelected(MouseEvent event) {
        if (event.getClickCount() == 2) {
            EquipmentDto selected = equipmentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEquipmentDetail(selected);
            }
        }
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 1) {
            loadPage(currentPage - 1);
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages) {
            loadPage(currentPage + 1);
        }
    }

    private void loadPage(int page) {
        setLoading(true);
        String search = searchField.getText().trim();
        String statusStr = statusFilter.getValue();
        EquipmentStatus status = null;
        if (statusStr != null && !statusStr.equals("All Statuses")) {
            try { status = EquipmentStatus.valueOf(statusStr); }
            catch (IllegalArgumentException ignored) { }
        }
        final EquipmentStatus finalStatus = status;

        Thread.ofVirtual().name("equip-load").start(() -> {
            try {
                ApiClient api = AppContext.get().getApiClient();
                PagedResult<EquipmentDto> result =
                        api.getEquipment(search, selectedLabId, page, PAGE_SIZE);
                Platform.runLater(() -> {
                    setLoading(false);
                    applyResult(result, page);
                });
            } catch (ApiException e) {
                LOG.error("Failed to load equipment: {}", e.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    UiUtils.showError("Load Error", e.getMessage());
                });
            }
        });
    }

    private void applyResult(PagedResult<EquipmentDto> result, int page) {
        equipmentTable.getItems().clear();
        if (result.getItems() != null) {
            equipmentTable.getItems().addAll(result.getItems());
        }
        currentPage = page;
        totalPages  = result.getTotalPages();
        pageInfoLabel.setText("Page " + page + " of " + totalPages
                + "  ·  " + result.getTotalItems() + " items");
        prevPageBtn.setDisable(page <= 1);
        nextPageBtn.setDisable(page >= totalPages);
    }

    private void setupTable() {
        colAssetCode.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getAssetCode()));
        colName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getName()));
        colCategory.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategoryName() != null
                        ? c.getValue().getCategoryName() : "—"));
        colLab.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getLabName() != null
                        ? c.getValue().getLabName() : "Unassigned"));
        colCondition.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCondition() != null
                        ? c.getValue().getCondition().name() : "—"));
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus() != null
                        ? c.getValue().getStatus().name() : "—"));
        colActions.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void setupFilters() {
        statusFilter.getItems().addAll(
                "All Statuses",
                EquipmentStatus.AVAILABLE.name(),
                EquipmentStatus.BORROWED.name(),
                EquipmentStatus.MAINTENANCE.name(),
                EquipmentStatus.RETIRED.name()
        );
        statusFilter.setValue("All Statuses");

        // Load labs for the lab filter
        labFilter.getItems().add("All Labs");
        labFilter.setValue("All Labs");
        labFilter.setOnAction(e -> {
            String selected = labFilter.getValue();
            selectedLabId = (selected == null || selected.equals("All Labs"))
                    ? null : labIdsByName.get(selected);
            onSearch();
        });

        Thread.ofVirtual().name("equip-labs-load").start(() -> {
            try {
                var labs = AppContext.get().getApiClient().getLabs(null, 1, 100);
                Platform.runLater(() -> {
                    if (labs.getItems() != null) {
                        for (LabDto lab : labs.getItems()) {
                            labIdsByName.put(lab.getName(), lab.getId());
                            labFilter.getItems().add(lab.getName());
                        }
                    }
                });
            } catch (ApiException ignored) { }
        });
    }

    private void showEquipmentDetail(EquipmentDto equipment) {
        com.labhub.client.app.ViewManager.get().showDialog(
                "EquipmentDetailDialog.fxml", "Equipment Detail",
                c -> ((EquipmentDetailDialogController) c).setEquipment(equipment));
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
    }
}
