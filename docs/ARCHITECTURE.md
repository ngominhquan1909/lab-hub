# LabHub - Architecture Documentation

## System Overview

LabHub is a **three-tier desktop client-server application**:

```
Client Tier       |   Network Tier        |   Data Tier
------------------+-----------------------+------------------
JavaFX UI         |   TCP Socket          |   MySQL 8
Controllers       |   JSON Protocol       |   HikariCP Pool
ApiClient         |   Port 9090 (default) |   Repositories
AppContext        |                       |   Domain Models
```

---

## Module Structure

### `labhub-common`
Shared code that both client and server depend on. Contains no business logic.

- **DTOs** - Data Transfer Objects for all entities (User, Lab, Equipment, Booking, etc.)
- **Enums** - `UserRole`, `EquipmentStatus`, `BookingStatus`, `BorrowStatus`, `MaintenanceStatus`, etc.
- **Protocol** - `Request`, `Response` envelopes; `RequestType` enum; `JsonMapper` singleton
- **Validation** - `ValidationResult` with field and global errors

### `labhub-server`
Java application running as a persistent background service.

```
LabHubServerApplication
+-- SocketServer (accept loop, fixed thread pool)
    +-- ClientHandler (one task per connection)
        +-- RequestDispatcher (authentication + routing)
            +-- AuthHandler         -> AuthService
            +-- BookingHandler      -> BookingService -> BookingRepository
            +-- BorrowHandler       -> BorrowService  -> BorrowRepository
            +-- EquipmentHandler    -> EquipmentRepository
            +-- LabHandler          -> LabRepository
            +-- MaintenanceHandler  -> MaintenanceRepository
            +-- UserHandler         -> UserRepository
            +-- DashboardHandler    -> DashboardRepository
```

**Layer responsibilities:**

| Layer | Responsibility |
|---|---|
| `network/` | TCP accept loop, per-client I/O, message framing |
| `handler/` | Deserialize payload, check authorization, call service, build response |
| `service/` | Business rules, validation, state machine transitions |
| `repository/` | SQL queries via JDBC PreparedStatement; never in handlers/services |
| `domain/` | Server-side entity classes (includes sensitive fields like `passwordHash`) |
| `security/` | BCrypt hashing, session token management |
| `config/` | Property loading, HikariCP pool initialization |

### `labhub-client`
JavaFX desktop application.

```
LabHubClientApplication (JavaFX Application)
+-- AppContext        - singleton: current user, ApiClient, ServerConnection
+-- ViewManager       - FXML loading, scene switching, dialog management
+-- ServerConnection  - raw TCP socket, newline-delimited JSON framing
+-- ApiClient         - high-level typed methods; wraps ServerConnection
+-- Controllers       - FXML-bound UI logic; call ApiClient on virtual threads
```

---

## Communication Protocol

**Transport:** TCP socket, persistent connection per client session.

**Framing:** Newline-delimited JSON - one complete JSON object per line, terminated by `\n`, UTF-8 encoded.

**Request envelope:**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "type":      "BOOKING_CREATE",
  "token":     "session-token-uuid",
  "payload":   { ... }
}
```

**Response envelope:**
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "success":   true,
  "code":      "OK",
  "message":   "Booking submitted successfully.",
  "payload":   { ... }
}
```

**Error codes:** `OK`, `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `DATABASE_ERROR`, `INTERNAL_ERROR`, `NETWORK_ERROR`, `INVALID_REQUEST`.

---

## Authentication & Authorization

1. Client sends `LOGIN` request (no token required).
2. Server validates credentials using BCrypt, creates a session token (UUID v4).
3. All subsequent requests include the token in the `token` field.
4. `RequestDispatcher` validates the token on every non-LOGIN request.
5. `SessionManager` maintains an in-memory token -> UserDto map with configurable TTL.
6. Authorization (role check) is performed in each handler using `RequestDispatcher.hasAnyRole()`.

**Security invariants:**
- Passwords are never stored in plaintext or logged anywhere.
- Error messages from login failures are generic (no username enumeration).
- Server-side validation duplicates all client-side validation.
- SQL injection is prevented by PreparedStatement throughout.
- Stack traces and DB error details never reach the client.

---

## Database Design

### Key design decisions

| Decision | Rationale |
|---|---|
| `BIGINT AUTO_INCREMENT` PKs | Simple, stable, no UUID fragmentation |
| Soft deletion (`is_active`) | Preserves history for equipment and users |
| All timestamps in UTC | Avoids timezone ambiguity; display conversion in client |
| `DECIMAL` for currency | Never `FLOAT` for monetary values |
| `VARCHAR` for enum columns | Human-readable in SQL; validated by application layer |
| Audit log has no FK on `actor_id` | Entries must persist even if user is deleted |
| Conflict detection in SQL | Single overlap query, checked at creation and again at approval |

### Entity relationships (abbreviated)

```
users --< user_roles >-- roles

labs --< equipment --< borrow_items >-- borrow_requests -- users
     --< bookings -- users

equipment --< maintenance_tickets -- users

audit_logs -- users (nullable FK)
```

---

## Booking Conflict Detection

The overlap condition for two intervals `[s1, e1)` and `[s2, e2)` is:
```
s1 < e2  AND  s2 < e1
```

Implemented in `BookingRepository.hasConflict()` as a single SQL query:
```sql
SELECT COUNT(*) FROM bookings
WHERE lab_id = ?
  AND status = 'APPROVED'
  AND start_time < ?   -- proposed end
  AND end_time   > ?   -- proposed start
  AND id != ?          -- exclude self for reschedule
```

The check is repeated at approval time as well as at creation. The check and the following update are separate statements, not one transaction.

---

## Equipment State Machine

```
AVAILABLE ----------------------------> IN_USE
    |                                        |
    +--> BORROWED (via BORROW_APPROVE)       |
    |         |                              |
    |         +--> AVAILABLE (BORROW_RETURN) |
    |                                        |
    +--> MAINTENANCE (MAINTENANCE_CREATE) <--+
    |         |
    |         +--> AVAILABLE (MAINTENANCE_CLOSE)
    |         +--> RETIRED
    |
    +--> RETIRED / LOST
```

---

## Threading Model

**Server:** One task per client connection on a fixed thread pool sized by `server.max.threads`. Each task blocks on `readLine()` and processes requests sequentially for that client. `SessionManager` and all handlers are designed to be safe for concurrent access from multiple threads.

**Client:** The JavaFX Application Thread handles all UI events. Network calls are dispatched to virtual threads. Results are posted back to the UI thread via `Platform.runLater()`. `ServerConnection` uses a `ReentrantLock` to serialize concurrent requests over the single socket.
