# LabHub — Implementation Status

**Last updated:** 2026-11-26  
**Build status:** Code generated — awaiting first local build verification  
**Test status:** Unit tests written — awaiting execution

---

## Current Milestone: M5 — UI & Integration

---

## Completed Tasks

### M0 — Design & Repository Setup ✅
- [x] Maven multi-module project structure (parent + 3 modules)
- [x] Parent POM with dependency management
- [x] Checkstyle configuration
- [x] `.gitignore`

### M1 — Common Module ✅
- [x] All enums: `UserRole`, `EquipmentStatus`, `EquipmentCondition`, `LabStatus`, `BookingStatus`, `BorrowStatus`, `MaintenancePriority`, `MaintenanceStatus`
- [x] All DTOs: `UserDto`, `LabDto`, `EquipmentDto`, `BookingDto`, `BorrowRequestDto` (with `BorrowItemDto`), `MaintenanceTicketDto`, `AuditLogDto`, `DashboardStatsDto`, `PagedResult<T>`, `CategoryDto`
- [x] Protocol: `Request`, `Response`, `RequestType` (exhaustive), `JsonMapper`
- [x] Validation: `ValidationResult` with field and global errors

### M2 — Database ✅
- [x] Complete MySQL schema (`database/schema.sql`)
  - `users`, `roles`, `user_roles`
  - `labs`, `equipment_categories`, `equipment`
  - `bookings`, `borrow_requests`, `borrow_items`
  - `maintenance_tickets`, `audit_logs`
  - All CHECK constraints, foreign keys, indexes
- [x] Realistic seed data (`database/seed.sql`)
  - 7 users across 4 roles
  - 5 laboratories
  - 8 equipment categories, 16 equipment items
  - 6 bookings (pending, approved, completed, rejected)
  - 3 borrow requests (active, pending, overdue)
  - 3 maintenance tickets (open, in-progress, closed)
  - 5 audit log entries

### M3 — Server ✅
- [x] `ServerConfig` — property loading with defaults
- [x] `DatabaseManager` — HikariCP pool setup
- [x] `SocketServer` — TCP accept loop, bounded thread pool
- [x] `ClientHandler` — newline-delimited JSON framing, per-client lifecycle
- [x] `RequestDispatcher` — authentication + routing
- [x] `RequestHandler` interface + `BaseHandler` base class
- [x] `SessionManager` — UUID token lifecycle, TTL, role checks
- [x] `PasswordHasher` — BCrypt (cost 12)
- [x] **Repositories:** `UserRepository`, `LabRepository`, `EquipmentRepository`, `BookingRepository`, `BorrowRepository`, `MaintenanceRepository`, `DashboardRepository`
- [x] **Domain:** `User` (server-side, includes passwordHash)
- [x] **Services:** `AuthService`, `BookingService`, `BorrowService`, `ServiceResult<T>`
- [x] **Handlers:** `AuthHandler`, `BookingHandler`, `BorrowHandler`, `LabHandler`, `EquipmentHandler`, `UserHandler`, `MaintenanceHandler`, `DashboardHandler`
- [x] `LabHubServerApplication` — wires everything, registers shutdown hook, starts background scheduler

### M4 — Client Network Layer ✅
- [x] `ServerConnection` — TCP socket, reconnect-ready
- [x] `ApiClient` — typed methods for all major operations
- [x] `ApiException` — structured error type
- [x] `AppContext` — session singleton
- [x] `ViewManager` — FXML loading and scene switching
- [x] `LabHubClientApplication` — JavaFX entry point

### M5 — Client UI ✅
- [x] Design system CSS (`labhub.css`) — editorial warm-paper palette
- [x] `LoginView.fxml` + `LoginController`
- [x] `MainView.fxml` + `MainController` (sidebar, navigation, page titles)
- [x] `DashboardView.fxml` + `DashboardController` (stat cards, today's schedule)
- [x] `LabsView.fxml` + `LabsController`
- [x] `EquipmentView.fxml` + `EquipmentController` (search, filter, pagination)
- [x] `BookingsView.fxml` + `BookingsController` (approve/reject inline actions)
- [x] `BorrowView.fxml` + `BorrowController` (approve/reject inline actions)
- [x] `MaintenanceView.fxml` + `MaintenanceController`
- [x] `UsersView.fxml` + `UsersController`
- [x] `AuditView.fxml` + `AuditController`
- [x] `UiUtils` — shared dialog helpers

### M6 — Tests ✅
- [x] `AuthServiceTest` — login, logout, change password (12 test cases)
- [x] `BookingServiceTest` — validation, approval, rejection, cancellation (11 test cases)
- [x] `SessionManagerTest` — session lifecycle and role checks (9 test cases)

### M7 — Documentation ✅
- [x] `README.md` — setup and execution instructions
- [x] `docs/ARCHITECTURE.md` — system design, protocol, threading model
- [x] `IMPLEMENTATION_STATUS.md` (this file)

---

## Tasks In Progress / Remaining

### High Priority — Required to build and run
- [ ] Verify `mvn clean package` succeeds — **not yet run in this environment** (no
      network access to Maven Central here); all changes below were verified by manual
      signature tracing (handler → service → repository → DTO → ApiClient → controller)
      and brace/paren/fx:id/onAction cross-checks instead. Run the real build before shipping.
- [ ] Fix any FXML controller binding issues found during first run
- [x] Wire `AuditController` to real `AUDIT_LIST` endpoint via `ApiClient` — done
      (`ApiClient.getAuditLogs()` + `AuditController` now calls it instead of faking an
      empty result)

### Medium Priority — Feature completeness
- [x] `NewBookingDialog.fxml` + controller — done, wired to `BookingsController.onNewBooking()`
- [x] `NewBorrowDialog.fxml` + controller — done (multi-select of AVAILABLE equipment),
      wired to `BorrowController.onNewRequest()`
- [x] `AddEquipmentDialog.fxml` + controller — done, wired to `EquipmentController.onAdd()`
- [x] `AddLabDialog.fxml` + controller — done, wired to `LabsController.onAddLab()`
- [x] `CreateUserDialog.fxml` + controller — done (role checkboxes), wired to
      `UsersController.onCreateUser()`; added `ApiClient.createUser()`
- [ ] Booking schedule / calendar view — server (`BOOKING_SCHEDULE` / `findSchedule`) was
      already complete; client-side calendar UI still not built
- [x] Equipment detail / history view — done: new `EQUIPMENT_HISTORY` server handler
      (was defined in `RequestType` but never implemented or registered — added a combined
      borrow + maintenance history query in `EquipmentHandler`, registered it in
      `LabHubServerApplication`), `ApiClient.getEquipmentHistory()`, and
      `EquipmentDetailDialog` replacing the old plain-text popup. Also fixed a pre-existing
      bug where the Equipment lab filter never mapped lab name → lab ID.
- [x] Maintenance ticket detail + action buttons (assign, resolve, close) — done via
      `MaintenanceTicketDetailDialog` (manager-only assign/resolve/close actions) and
      `NewMaintenanceTicketDialog` (file a new ticket); added
      `ApiClient.assignMaintenanceTicket()/resolveMaintenanceTicket()/closeMaintenanceTicket()`
- [x] Return equipment dialog (with per-item condition recording) — done via the shared
      `ItemConditionDialog` (used for both checkout and return); added
      `ApiClient.checkoutBorrowRequest()/cancelBorrowRequest()`. Also added working
      Checkout/Return/Cancel row actions to `BorrowController` and a working Cancel action
      to `BookingsController` (the latter had a comment saying "cancel for owners" but no
      button was ever wired).

### Low Priority — Polish
- [x] Pagination for the AuditController — done, see above
- [ ] Keyboard shortcuts for common actions
- [ ] Export to CSV for equipment and booking lists
- [ ] Session expiry notification in the client
- [ ] Reconnection handling when server becomes unavailable

---

## Known Issues

| # | Description | Severity | Workaround |
|---|---|---|---|
| 1 | `BookingServiceTest` uses inner fake classes that call `super(null)` — may cause NPE if any base repository method is called without being overridden | Medium | All relevant methods are overridden in fakes |
| 2 | ~~`AuditController` returns empty results~~ — **Fixed**: now wired to `ApiClient.getAuditLogs()` | — | — |
| 3 | ~~`BorrowHandler.handleCancel()` returns `NOT_IMPLEMENTED`~~ — **Fixed**: added `BorrowRepository.cancel()` / `BorrowService.cancel()`, handles PENDING and APPROVED requests, releases reserved equipment back to AVAILABLE | — | — |
| 4 | `JavaFX 25.0.2` not yet on Maven Central; using `21.0.4 LTS` | Note | API-compatible; update version when 25.x is published |
| 5 | No `mvn clean package` run against these changes — this sandbox cannot reach Maven Central/the dependency repos | Medium | Run the build locally before relying on this status doc; this session's checks were manual (signature tracing + structural cross-checks), not a compiler |

---

## Technical Decisions

| Decision | Rationale |
|---|---|
| JavaFX 21.0.4 instead of 25.0.2 | 25.x not on Maven Central; 21 LTS is fully compatible |
| `Thread.ofVirtual()` for client async | Available from Java 21; keeps network calls off the UI thread cleanly |
| `ReentrantLock` on `ServerConnection` | Single persistent connection per client; lock serializes request/response cycles |
| Hand-written fakes instead of Mockito | Avoids adding Mockito to the dependency set; keeps test intent explicit |
| `PagedResult<T>` without Jackson generics annotation | Deserialization handled manually in `ApiClient.pagedResult()` to avoid type erasure issues |
| `--enable-preview` compiler flag | Required for some Java 25 features; harmless on 21 |

---

## Build Verification Checklist

Before declaring the project complete, verify:

- [ ] `mvn clean compile` — no errors in all 3 modules
- [ ] `mvn test` — all unit tests pass
- [ ] `mysql < database/schema.sql` — no SQL errors
- [ ] `mysql < database/seed.sql` — no SQL errors, expected row counts verified
- [ ] Server starts and prints `LabHub Server ready` log line
- [ ] Client starts and shows the login screen
- [ ] Login with `admin` / `admin123` succeeds
- [ ] Dashboard stats load from the database
- [ ] Equipment list displays all 16 seed items
- [ ] Booking approval flow: pending → approved
- [ ] Borrow request flow: pending → approved → active → returned
- [ ] Maintenance ticket flow: open → in-progress → resolved → closed

---

## File Count Summary

| Module | Java files | FXML | CSS | SQL | Other |
|---|---|---|---|---|---|
| `labhub-common` | 15 | — | — | — | 1 POM |
| `labhub-server` | 25 | — | — | — | 1 POM, 2 config |
| `labhub-client` | 14 | 9 | 1 | — | 1 POM, 2 config |
| `database/` | — | — | — | 2 | — |
| **Total** | **54** | **9** | **1** | **2** | — |
