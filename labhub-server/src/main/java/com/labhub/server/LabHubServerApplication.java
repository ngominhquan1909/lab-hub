package com.labhub.server;

import com.labhub.common.protocol.RequestType;
import com.labhub.server.config.DatabaseManager;
import com.labhub.server.config.ServerConfig;
import com.labhub.server.handler.AuthHandler;
import com.labhub.server.handler.BookingHandler;
import com.labhub.server.handler.BorrowHandler;
import com.labhub.server.handler.DashboardHandler;
import com.labhub.server.handler.EquipmentHandler;
import com.labhub.server.handler.LabHandler;
import com.labhub.server.handler.MaintenanceHandler;
import com.labhub.server.handler.RequestDispatcher;
import com.labhub.server.handler.UserHandler;
import com.labhub.server.network.SocketServer;
import com.labhub.server.repository.BookingRepository;
import com.labhub.server.repository.BorrowRepository;
import com.labhub.server.repository.DashboardRepository;
import com.labhub.server.repository.EquipmentRepository;
import com.labhub.server.repository.LabRepository;
import com.labhub.server.repository.MaintenanceRepository;
import com.labhub.server.repository.UserRepository;
import com.labhub.server.security.SessionManager;
import com.labhub.server.service.AuthService;
import com.labhub.server.service.BookingService;
import com.labhub.server.service.BorrowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * LabHub Server entry point.
 *
 * <p>Startup sequence:
 * <ol>
 *   <li>Load configuration.</li>
 *   <li>Connect to database (fail-fast if unavailable).</li>
 *   <li>Wire repositories → services → handlers → dispatcher.</li>
 *   <li>Register shutdown hook for graceful cleanup.</li>
 *   <li>Start background maintenance tasks.</li>
 *   <li>Start the socket server (blocks until shutdown).</li>
 * </ol>
 *
 * <p>Usage: {@code java -jar labhub-server.jar [path/to/server.properties]}
 */
public class LabHubServerApplication {

    private static final Logger LOG = LoggerFactory.getLogger(LabHubServerApplication.class);

    /**
     * Application entry point.
     *
     * @param args optional: path to server.properties as first argument
     */
    public static void main(String[] args) {
        LOG.info("=== LabHub Server starting ===");

        // ── 1. Configuration ──────────────────────────────────────────────
        ServerConfig config = ServerConfig.load(args);

        // ── 2. Database ───────────────────────────────────────────────────
        DatabaseManager db;
        try {
            db = new DatabaseManager(config);
        } catch (IllegalStateException e) {
            LOG.error("FATAL: Cannot connect to database. {}", e.getMessage());
            System.exit(1);
            return; // unreachable; silences compiler warning
        }

        // ── 3. Repositories ───────────────────────────────────────────────
        UserRepository        userRepo        = new UserRepository(db);
        LabRepository         labRepo         = new LabRepository(db);
        EquipmentRepository   equipmentRepo   = new EquipmentRepository(db);
        BookingRepository     bookingRepo     = new BookingRepository(db);
        BorrowRepository      borrowRepo      = new BorrowRepository(db);
        MaintenanceRepository maintenanceRepo = new MaintenanceRepository(db);
        DashboardRepository   dashboardRepo   = new DashboardRepository(db);

        // ── 4. Session manager ────────────────────────────────────────────
        SessionManager sessionManager = new SessionManager(config.getSessionTimeoutMinutes());

        // ── 5. Services ───────────────────────────────────────────────────
        AuthService    authService    = new AuthService(userRepo, sessionManager);
        BookingService bookingService = new BookingService(bookingRepo, labRepo, dashboardRepo, config);
        BorrowService  borrowService  = new BorrowService(borrowRepo, equipmentRepo, dashboardRepo);

        // ── 6. Handlers ───────────────────────────────────────────────────
        AuthHandler        authHandler        = new AuthHandler(authService);
        BookingHandler     bookingHandler     = new BookingHandler(bookingService, bookingRepo);
        BorrowHandler      borrowHandler      = new BorrowHandler(borrowService);
        DashboardHandler   dashboardHandler   = new DashboardHandler(dashboardRepo);
        LabHandler         labHandler         = new LabHandler(labRepo, dashboardRepo);
        EquipmentHandler   equipmentHandler   = new EquipmentHandler(equipmentRepo, dashboardRepo, db);
        UserHandler        userHandler        = new UserHandler(userRepo, dashboardRepo);
        MaintenanceHandler maintenanceHandler = new MaintenanceHandler(maintenanceRepo, equipmentRepo, dashboardRepo);

        // ── 7. Dispatcher ─────────────────────────────────────────────────
        RequestDispatcher dispatcher = new RequestDispatcher(sessionManager);

        // Auth
        dispatcher.register(RequestType.LOGIN,           authHandler);
        dispatcher.register(RequestType.LOGOUT,          authHandler);
        dispatcher.register(RequestType.CHANGE_PASSWORD, authHandler);

        // Users
        dispatcher.register(RequestType.USER_LIST,    userHandler);
        dispatcher.register(RequestType.USER_GET,     userHandler);
        dispatcher.register(RequestType.USER_CREATE,  userHandler);
        dispatcher.register(RequestType.USER_UPDATE,  userHandler);
        dispatcher.register(RequestType.USER_DISABLE, userHandler);
        dispatcher.register(RequestType.USER_ENABLE,  userHandler);

        // Labs
        dispatcher.register(RequestType.LAB_LIST,       labHandler);
        dispatcher.register(RequestType.LAB_GET,        labHandler);
        dispatcher.register(RequestType.LAB_CREATE,     labHandler);
        dispatcher.register(RequestType.LAB_UPDATE,     labHandler);
        dispatcher.register(RequestType.LAB_DEACTIVATE, labHandler);

        // Equipment
        dispatcher.register(RequestType.EQUIPMENT_LIST,       equipmentHandler);
        dispatcher.register(RequestType.EQUIPMENT_GET,        equipmentHandler);
        dispatcher.register(RequestType.EQUIPMENT_CREATE,     equipmentHandler);
        dispatcher.register(RequestType.EQUIPMENT_UPDATE,     equipmentHandler);
        dispatcher.register(RequestType.EQUIPMENT_DEACTIVATE, equipmentHandler);
        dispatcher.register(RequestType.CATEGORY_LIST,        equipmentHandler);
        dispatcher.register(RequestType.CATEGORY_CREATE,      equipmentHandler);

        // Bookings
        dispatcher.register(RequestType.BOOKING_LIST,     bookingHandler);
        dispatcher.register(RequestType.BOOKING_GET,      bookingHandler);
        dispatcher.register(RequestType.BOOKING_CREATE,   bookingHandler);
        dispatcher.register(RequestType.BOOKING_APPROVE,  bookingHandler);
        dispatcher.register(RequestType.BOOKING_REJECT,   bookingHandler);
        dispatcher.register(RequestType.BOOKING_CANCEL,   bookingHandler);
        dispatcher.register(RequestType.BOOKING_SCHEDULE, bookingHandler);

        // Borrows
        dispatcher.register(RequestType.BORROW_LIST,     borrowHandler);
        dispatcher.register(RequestType.BORROW_GET,      borrowHandler);
        dispatcher.register(RequestType.BORROW_CREATE,   borrowHandler);
        dispatcher.register(RequestType.BORROW_APPROVE,  borrowHandler);
        dispatcher.register(RequestType.BORROW_REJECT,   borrowHandler);
        dispatcher.register(RequestType.BORROW_CHECKOUT, borrowHandler);
        dispatcher.register(RequestType.BORROW_RETURN,   borrowHandler);
        dispatcher.register(RequestType.BORROW_CANCEL,   borrowHandler);

        // Maintenance
        dispatcher.register(RequestType.MAINTENANCE_LIST,          maintenanceHandler);
        dispatcher.register(RequestType.MAINTENANCE_GET,           maintenanceHandler);
        dispatcher.register(RequestType.MAINTENANCE_CREATE,        maintenanceHandler);
        dispatcher.register(RequestType.MAINTENANCE_UPDATE_STATUS, maintenanceHandler);
        dispatcher.register(RequestType.MAINTENANCE_ASSIGN,        maintenanceHandler);
        dispatcher.register(RequestType.MAINTENANCE_RESOLVE,       maintenanceHandler);
        dispatcher.register(RequestType.MAINTENANCE_CLOSE,         maintenanceHandler);

        // Dashboard
        dispatcher.register(RequestType.DASHBOARD_STATS,    dashboardHandler);
        dispatcher.register(RequestType.DASHBOARD_ACTIVITY, dashboardHandler);
        dispatcher.register(RequestType.AUDIT_LIST,         dashboardHandler);

        // ── 8. Socket server ──────────────────────────────────────────────
        SocketServer server = new SocketServer(config, dispatcher);

        // ── 9. Shutdown hook ──────────────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutdown signal received.");
            server.stop();
            db.close();
            LOG.info("LabHub Server shut down cleanly.");
        }, "shutdown-hook"));

        // ── 10. Background tasks ──────────────────────────────────────────
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "labhub-scheduler"));

        // Evict expired sessions every 15 minutes
        scheduler.scheduleAtFixedRate(sessionManager::evictExpired, 15, 15, TimeUnit.MINUTES);

        // Mark overdue borrow requests every 30 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int count = borrowRepo.markOverdue();
                if (count > 0) {
                    LOG.info("Marked {} borrow request(s) as OVERDUE.", count);
                }
            } catch (Exception e) {
                LOG.warn("Overdue marking task failed: {}", e.getMessage());
            }
        }, 1, 30, TimeUnit.MINUTES);

        // ── 11. Start ─────────────────────────────────────────────────────
        try {
            LOG.info("=== LabHub Server ready ===");
            server.start(); // blocks until shutdown
        } catch (IOException e) {
            LOG.error("FATAL: Failed to start server: {}", e.getMessage(), e);
            scheduler.shutdownNow();
            db.close();
            System.exit(2);
        }

        scheduler.shutdownNow();
    }
}
