package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.network.ApiException;
import com.labhub.client.util.UiUtils;
import com.labhub.common.dto.PagedResult;
import com.labhub.common.dto.UserDto;
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

/** Controller for the User Accounts view (admin only). */
public class UsersController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(UsersController.class);
    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @FXML private TextField searchField;
    @FXML private HBox      loadingBox;

    @FXML private TableView<UserDto>              usersTable;
    @FXML private TableColumn<UserDto, String>   colUsername;
    @FXML private TableColumn<UserDto, String>   colName;
    @FXML private TableColumn<UserDto, String>   colEmail;
    @FXML private TableColumn<UserDto, String>   colRoles;
    @FXML private TableColumn<UserDto, String>   colStatus;
    @FXML private TableColumn<UserDto, String>   colLastLogin;
    @FXML private TableColumn<UserDto, String>   colActions;

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

    @FXML
    private void onCreateUser() {
        Object controller = com.labhub.client.app.ViewManager.get()
                .showDialog("CreateUserDialog.fxml", "Create User Account");
        if (controller instanceof CreateUserDialogController dialog
                && dialog.getCreatedUser() != null) {
            loadPage(1);
        }
    }

    @FXML private void onPrevPage() { if (currentPage > 1) loadPage(currentPage - 1); }
    @FXML private void onNextPage() { if (currentPage < totalPages) loadPage(currentPage + 1); }

    private void loadPage(int page) {
        setLoading(true);
        String search = searchField.getText().trim();
        Thread.ofVirtual().name("users-load").start(() -> {
            try {
                PagedResult<UserDto> result =
                        AppContext.get().getApiClient().getUsers(search, page, PAGE_SIZE);
                Platform.runLater(() -> { setLoading(false); applyResult(result, page); });
            } catch (ApiException ex) {
                LOG.error("Failed to load users: {}", ex.getMessage());
                Platform.runLater(() -> {
                    setLoading(false);
                    UiUtils.showError("Error", ex.getMessage());
                });
            }
        });
    }

    private void applyResult(PagedResult<UserDto> result, int page) {
        usersTable.getItems().clear();
        if (result.getItems() != null) {
            usersTable.getItems().addAll(result.getItems());
        }
        currentPage = page;
        totalPages  = result.getTotalPages();
        pageInfoLabel.setText("Page " + page + " of " + totalPages
                + "  ·  " + result.getTotalItems() + " users");
        prevBtn.setDisable(page <= 1);
        nextBtn.setDisable(page >= totalPages);
    }

    private void setupTable() {
        colUsername.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsername()));
        colName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getFullName()));
        colEmail.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEmail()));
        colRoles.setCellValueFactory(c -> {
            if (c.getValue().getRoles() == null) return new SimpleStringProperty("—");
            String roles = c.getValue().getRoles().stream()
                    .map(r -> r.name().replace("_", " "))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("—");
            return new SimpleStringProperty(roles);
        });
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isActive() ? "Active" : "Disabled"));
        colLastLogin.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getLastLoginAt() != null
                        ? c.getValue().getLastLoginAt().format(DT_FMT) : "Never"));
        colActions.setCellValueFactory(c -> new SimpleStringProperty(""));
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
    }
}
