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
- [ ] Verify `mvn clean package` succeeds (compile errors may exist in generated code)
- [ ] Fix any FXML controller binding issues found during first run
- [ ] Wire `AuditController` to real `AUDIT_LIST` endpoint via `ApiClient`

### Medium Priority — Feature completeness
- [ ] `NewBookingDialog.fxml` + controller (booking creation form with date/time pickers)
- [ ] `NewBorrowDialog.fxml` + controller (equipment selection + return date)
- [ ] `AddEquipmentDialog.fxml` + controller
- [ ] `AddLabDialog.fxml` + controller
- [ ] `CreateUserDialog.fxml` + controller
- [ ] Booking schedule / calendar view
- [ ] Equipment detail / history view
- [ ] Maintenance ticket detail + action buttons (assign, resolve, close)
- [ ] Return equipment dialog (with per-item condition recording)

### Low Priority — Polish
- [ ] Pagination for the AuditController (needs an `auditList` method in `ApiClient`)
- [ ] Keyboard shortcuts for common actions
- [ ] Export to CSV for equipment and booking lists
- [ ] Session expiry notification in the client
- [ ] Reconnection handling when server becomes unavailable

---

## Known Issues

| # | Description | Severity | Workaround |
|---|---|---|---|
| 1 | `BookingServiceTest` uses inner fake classes that call `super(null)` — may cause NPE if any base repository method is called without being overridden | Medium | All relevant methods are overridden in fakes |
| 2 | `AuditController` returns empty results (endpoint not wired in `ApiClient`) | Low | Audit data visible in DB; full wiring is straightforward |
| 3 | `BorrowHandler.handleCancel()` returns `NOT_IMPLEMENTED` | Low | Cancellation can be performed by manager via status update |
| 4 | `JavaFX 25.0.2` not yet on Maven Central; using `21.0.4 LTS` | Note | API-compatible; update version when 25.x is published |

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
