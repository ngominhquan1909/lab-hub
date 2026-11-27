# LabHub - Laboratory Operations Management System

A client-server desktop application for managing university laboratory rooms, equipment inventory, booking reservations, and equipment loan requests.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 21 LTS or later |
| Apache Maven | 3.8+ |
| MySQL | 8.0+ |

> **Note on Java version:** The parent POM specifies Java 25 as the target per the project specification. If Java 25 is not yet available on your machine, Java 21 LTS compiles and runs the project correctly - set `<java.version>21</java.version>` in the parent `pom.xml` and remove `--enable-preview` from the compiler args.

---

## Quick Start

### 1. Database setup

```bash
# Log in to MySQL
mysql -u root -p

# Run the schema script
source /path/to/labhub/database/schema.sql

# Run the seed data script
source /path/to/labhub/database/seed.sql
```

### 2. Server configuration

```bash
cd labhub-server/src/main/resources
cp server.properties.example server.properties
```

Edit `server.properties`:

```properties
server.host=localhost
server.port=9090

db.host=localhost
db.port=3306
db.name=labhub
db.username=labhub_user     # or root for local dev
db.password=YOUR_PASSWORD
```

### 3. Client configuration

```bash
cd labhub-client/src/main/resources
cp client.properties.example client.properties
```

Edit `client.properties`:

```properties
server.host=localhost
server.port=9090
```

### 4. Build

```bash
# From the project root
mvn clean package -DskipTests
```

### 5. Run the server

```bash
# Option A: via Maven
cd labhub-server
mvn exec:java -Dexec.mainClass="com.labhub.server.LabHubServerApplication"

# Option B: fat jar (after package)
java -jar labhub-server/target/labhub-server-1.0.0-SNAPSHOT.jar
```

### 6. Run the client

```bash
# Option A: via Maven JavaFX plugin
cd labhub-client
mvn javafx:run

# Option B: with module path (if JavaFX is installed separately)
java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml \
     -jar labhub-client/target/labhub-client-1.0.0-SNAPSHOT.jar
```

---

## Seed Accounts

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | Admin + Lab Manager |
| `dr.nam` | `manager123` | Lab Manager |
| `prof.linh` | `lecturer123` | Lecturer |
| `mr.hieu` | `lecturer123` | Lecturer |
| `quan.tran` | `student123` | Student |
| `anh.nguyen` | `student123` | Student |
| `duc.pham` | `student123` | Student |

> **Change all passwords before any real use.**

---

## Project Structure

```
labhub/
+-- labhub-common/          # Shared: DTOs, enums, protocol, validation
+-- labhub-server/          # Server: socket server, services, repositories, DB
+-- labhub-client/          # Client: JavaFX UI, controllers, API client
+-- database/
|   +-- schema.sql          # MySQL schema (run first)
|   +-- seed.sql            # Demo data (run after schema)
+-- docs/                   # Architecture and design documentation
+-- checkstyle.xml          # Code style configuration
+-- pom.xml                 # Parent Maven POM
```

---

## Running Tests

```bash
# All tests (unit tests only - no DB required)
mvn test

# Server module tests only
cd labhub-server && mvn test

# Common module tests only
cd labhub-common && mvn test
```

Tests use hand-written fakes (no Mockito) so they run without a database or server.

---

## Architecture Overview

```
+------------------------------------------------------+
|                    JavaFX Client                     |
|  Controllers -> ApiClient -> ServerConnection (TCP)  |
+---------------------------+--------------------------+
                            |  TCP socket (newline-delimited JSON)
+---------------------------v--------------------------+
|                    LabHub Server                     |
|  SocketServer -> ClientHandler -> RequestDispatcher  |
|    -> Handlers -> Services -> Repositories -> MySQL  |
+------------------------------------------------------+
```

See `docs/ARCHITECTURE.md` for full details.

---

## Technology Decisions

| Decision | Rationale |
|---|---|
| JavaFX 21.0.4 LTS instead of 25.0.2 | JavaFX 25 is not yet available on Maven Central. JavaFX 21 LTS is ABI-compatible and production-stable. |
| BCrypt cost factor 12 | ~300ms on modern hardware. Appropriate for a university desktop app. |
| Newline-delimited JSON protocol | Simple, debuggable with `telnet`/`nc`, avoids message framing complexity. |
| HikariCP | Industry-standard connection pool, minimal configuration. |
| Hand-written fakes for tests | Avoids Mockito dependency; keeps the test surface explicit and readable. |
| Virtual threads (`Thread.ofVirtual()`) | Keeps network calls off the JavaFX thread without callback hell; available from Java 21. |
