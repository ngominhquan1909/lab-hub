package com.labhub.client.controller;

import com.labhub.client.app.AppContext;
import com.labhub.client.app.ViewManager;
import com.labhub.client.network.ApiException;
import com.labhub.common.dto.UserDto;
import com.labhub.common.enums.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the main application layout.
 * Manages sidebar navigation, page title, and content area swapping.
 */
public class MainController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    // Sidebar nav buttons
    @FXML private Button navDashboard;
    @FXML private Button navLabs;
    @FXML private Button navEquipment;
    @FXML private Button navBookings;
    @FXML private Button navBorrow;
    @FXML private Button navMaintenance;
    @FXML private Button navUsers;
    @FXML private Button navAudit;
    @FXML private Label  adminSectionLabel;

    // Top bar
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label pendingBadgeLabel;
    @FXML private javafx.scene.layout.HBox pendingBadgeBox;

    // User info
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;

    // Content area
    @FXML private StackPane contentPane;

    private Button activeNavButton;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Show user info
        UserDto user = AppContext.get().getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            String roles = user.getRoles() != null
                    ? user.getRoles().stream()
                            .map(r -> r.name().replace("_", " "))
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("—")
                    : "—";
            userRoleLabel.setText(roles);
        }

        // Show admin section for admins
        boolean isAdmin = AppContext.get().isAdmin();
        boolean isManager = AppContext.get().isManager();
        adminSectionLabel.setVisible(isAdmin);
        adminSectionLabel.setManaged(isAdmin);
        navUsers.setVisible(isAdmin);
        navUsers.setManaged(isAdmin);
        navAudit.setVisible(isManager);
        navAudit.setManaged(isManager);

        // Navigate to dashboard on startup
        navigateTo(navDashboard, "Dashboard", "", "DashboardView.fxml");
    }

    // ── Navigation handler ────────────────────────────────────────────────

    @FXML
    private void onNav(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        if (btn == navDashboard) {
            navigateTo(btn, "Dashboard", "Operational overview", "DashboardView.fxml");
        } else if (btn == navLabs) {
            navigateTo(btn, "Laboratories", "Manage lab rooms and spaces", "LabsView.fxml");
        } else if (btn == navEquipment) {
            navigateTo(btn, "Equipment", "Inventory and asset management", "EquipmentView.fxml");
        } else if (btn == navBookings) {
            navigateTo(btn, "Room Bookings", "Lab reservations and schedule", "BookingsView.fxml");
        } else if (btn == navBorrow) {
            navigateTo(btn, "Borrow Requests", "Equipment loan management", "BorrowView.fxml");
        } else if (btn == navMaintenance) {
            navigateTo(btn, "Maintenance", "Equipment service and repair tickets", "MaintenanceView.fxml");
        } else if (btn == navUsers) {
            navigateTo(btn, "User Accounts", "Account management", "UsersView.fxml");
        } else if (btn == navAudit) {
            navigateTo(btn, "Audit Log", "System activity history", "AuditView.fxml");
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @FXML
    private void onLogout() {
        // Invalidate session on server (best-effort)
        Thread.ofVirtual().name("logout-thread").start(() -> {
            try {
                AppContext.get().getApiClient().logout();
            } catch (ApiException e) {
                LOG.warn("Logout request failed (server may be down): {}", e.getMessage());
            } finally {
                Platform.runLater(() -> {
                    AppContext.get().clearSession();
                    ViewManager.get().showLoginScreen();
                });
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Swaps the content area and updates the active nav button state.
     */
    private void navigateTo(Button btn, String title, String subtitle, String fxmlName) {
        // Update active button style
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove("active");
        }
        btn.getStyleClass().add("active");
        activeNavButton = btn;

        // Update page title
        pageTitleLabel.setText(title);
        pageSubtitleLabel.setText(subtitle);

        // Load content
        ViewManager.get().showContentPane(contentPane, fxmlName);
        LOG.debug("Navigated to: {}", fxmlName);
    }

    /**
     * Updates the pending items badge in the top bar.
     * Called by child controllers (e.g. DashboardController) after loading stats.
     *
     * @param pendingCount total pending bookings + borrows
     */
    public void updatePendingBadge(int pendingCount) {
        if (pendingCount > 0) {
            pendingBadgeLabel.setText(pendingCount + " pending");
            pendingBadgeBox.setVisible(true);
            pendingBadgeBox.setManaged(true);
        } else {
            pendingBadgeBox.setVisible(false);
            pendingBadgeBox.setManaged(false);
        }
    }
}
