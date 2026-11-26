package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.client.util.UiUtils;
import com.labhub.common.dto.LabDto;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/** Controller for the Labs view. */
public class LabsController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(LabsController.class);
    private static final int PAGE_SIZE = 20;

    @FXML private TextField searchField;
    @FXML private Button    addLabBtn;
    @FXML private HBox      loadingBox;

    @FXML private TableView<LabDto>              labsTable;
    @FXML private TableColumn<LabDto, String>   colCode;
    @FXML private TableColumn<LabDto, String>   colName;
    @FXML private TableColumn<LabDto, String>   colBuilding;
    @FXML private TableColumn<LabDto, String>   colFloor;
    @FXML private TableColumn<LabDto, String>   colCapacity;
    @FXML private TableColumn<LabDto, String>   colType;
    @FXML private TableColumn<LabDto, String>   colEquipCnt;
    @FXML private TableColumn<LabDto, String>   colStatus;

    @FXML private Label pageInfoLabel;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;

    private int currentPage = 1;
    private int totalPages  = 1;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        if (!AppContext.get().isManager()) {
            addLabBtn.setVisible(false);
            addLabBtn.setManaged(false);
        }
        loadPage(1);
    }

    @FXML private void onSearch() { loadPage(1); }

    @FXML
    private void onAddLab() {
        UiUtils.showInfo("Add Laboratory", "Lab creation dialog opens here.");
    }

    @FXML private void onRowClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            LabDto sel = labsTable.getSelectionModel().getSelectedItem();
            if (sel != null) showDetail(sel);
        }
    }

    @FXML private void onPrevPage() { if (currentPage > 1) loadPage(currentPage - 1); }
    @FXML private void onNextPage() { if (currentPage < totalPages) loadPage(currentPage + 1); }

    private void loadPage(int page) {
        setLoading(true);
        String search = searchField.getText().trim();
        Thread.ofVirtual().name("labs-load").start(() -> {
            try {
                PagedResult<LabDto> result =
                        AppContext.get().getApiClient().getLabs(search, page, PAGE_SIZE);
                Platform.runLater(() -> { setLoading(false); applyResult(result, page); });
            } catch (ApiException ex) {
                LOG.error("Failed to load labs: {}", ex.getMessage());
                Platform.runLater(() -> { setLoading(false); UiUtils.showError("Error", ex.getMessage()); });
            }
        });
    }

    private void applyResult(PagedResult<LabDto> result, int page) {
        labsTable.getItems().clear();
        if (result.getItems() != null) labsTable.getItems().addAll(result.getItems());
        currentPage = page; totalPages = result.getTotalPages();
        pageInfoLabel.setText("Page " + page + " of " + totalPages
                + "  ·  " + result.getTotalItems() + " labs");
        prevBtn.setDisable(page <= 1);
        nextBtn.setDisable(page >= totalPages);
    }

    private void setupTable() {
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLabCode()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colBuilding.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBuilding()));
        colFloor.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getFloor() != null ? String.valueOf(c.getValue().getFloor()) : "—"));
        colCapacity.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCapacity() != null ? String.valueOf(c.getValue().getCapacity()) : "—"));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRoomType()));
        colEquipCnt.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEquipmentCount() != null
                        ? String.valueOf(c.getValue().getEquipmentCount()) : "—"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() != null ? c.getValue().getStatus().name() : "—"));
    }

    private void showDetail(LabDto l) {
        UiUtils.showInfo("Lab: " + l.getName(),
                "Code:     " + l.getLabCode()
                + "\nBuilding: " + l.getBuilding()
                + "\nFloor:    " + l.getFloor()
                + "\nCapacity: " + l.getCapacity()
                + "\nType:     " + l.getRoomType()
                + "\nEquip:    " + l.getEquipmentCount()
                + "\nStatus:   " + (l.getStatus() != null ? l.getStatus().name() : "—")
                + (l.getDescription() != null ? "\n\n" + l.getDescription() : ""));
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading); loadingBox.setManaged(loading);
    }
}
