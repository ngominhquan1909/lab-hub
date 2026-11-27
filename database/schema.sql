-- ============================================================
-- LabHub - Database Schema
-- MySQL 8.0+
-- Timezone strategy: all timestamps stored as UTC (DATETIME columns).
-- ID strategy: BIGINT AUTO_INCREMENT (simple, stable, non-guessable
--              in context of this desktop app).
-- Soft deletion: records with business history are deactivated,
--                not hard-deleted.
-- ============================================================

CREATE DATABASE IF NOT EXISTS labhub
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE labhub;

-- Users

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    phone_number  VARCHAR(30),
    password_hash VARCHAR(255) NOT NULL,  -- BCrypt hash; plaintext is NEVER stored
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT (UTC_TIMESTAMP()),
    updated_at    DATETIME     NOT NULL DEFAULT (UTC_TIMESTAMP()) ON UPDATE CURRENT_TIMESTAMP,
    last_login_at DATETIME,

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email    (email),
    INDEX idx_users_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Roles
-- Role values must match com.labhub.common.enums.UserRole exactly.

CREATE TABLE roles (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Laboratories

CREATE TABLE labs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    lab_code    VARCHAR(30)  NOT NULL,
    name        VARCHAR(150) NOT NULL,
    building    VARCHAR(100) NOT NULL,
    floor       INT          NOT NULL DEFAULT 1,
    capacity    INT          NOT NULL,
    room_type   VARCHAR(80)  NOT NULL,
    description TEXT,
    status      VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    -- status CHECK: ACTIVE | INACTIVE | DECOMMISSIONED
    created_at  DATETIME     NOT NULL DEFAULT (UTC_TIMESTAMP()),
    updated_at  DATETIME     NOT NULL DEFAULT (UTC_TIMESTAMP()) ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_labs_code (lab_code),
    INDEX idx_labs_status (status),
    CONSTRAINT chk_labs_capacity CHECK (capacity > 0),
    CONSTRAINT chk_labs_status   CHECK (status IN ('ACTIVE','INACTIVE','DECOMMISSIONED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Equipment categories

CREATE TABLE equipment_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description TEXT,

    PRIMARY KEY (id),
    UNIQUE KEY uq_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Equipment

CREATE TABLE equipment (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    asset_code       VARCHAR(50)    NOT NULL,
    name             VARCHAR(200)   NOT NULL,
    category_id      BIGINT         NOT NULL,
    manufacturer     VARCHAR(100),
    model            VARCHAR(100),
    serial_number    VARCHAR(100),
    lab_id           BIGINT,              -- nullable: unassigned equipment
    acquisition_date DATE,
    purchase_cost    DECIMAL(12, 2),      -- DECIMAL, never FLOAT, for currency
    condition_state  VARCHAR(20)    NOT NULL DEFAULT 'GOOD',
    status           VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    notes            TEXT,
    is_active        TINYINT(1)     NOT NULL DEFAULT 1,
    created_at       DATETIME       NOT NULL DEFAULT (UTC_TIMESTAMP()),
    updated_at       DATETIME       NOT NULL DEFAULT (UTC_TIMESTAMP()) ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_equipment_asset_code   (asset_code),
    UNIQUE KEY uq_equipment_serial_number (serial_number),  -- partial uniqueness: NULLs are allowed
    INDEX idx_equipment_lab      (lab_id),
    INDEX idx_equipment_category (category_id),
    INDEX idx_equipment_status   (status),
    INDEX idx_equipment_active   (is_active),
    CONSTRAINT fk_equipment_category FOREIGN KEY (category_id) REFERENCES equipment_categories(id),
    CONSTRAINT fk_equipment_lab      FOREIGN KEY (lab_id)      REFERENCES labs(id),
    CONSTRAINT chk_equipment_condition CHECK (condition_state IN ('EXCELLENT','GOOD','FAIR','POOR')),
    CONSTRAINT chk_equipment_status   CHECK (status IN ('AVAILABLE','IN_USE','BORROWED','MAINTENANCE','LOST','RETIRED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bookings

CREATE TABLE bookings (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    lab_id         BIGINT      NOT NULL,
    requester_id   BIGINT      NOT NULL,
    start_time     DATETIME    NOT NULL,
    end_time       DATETIME    NOT NULL,
    purpose        VARCHAR(500) NOT NULL,
    attendee_count INT         NOT NULL,
    notes          TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by_id  BIGINT,
    decided_at     DATETIME,
    decision_note  TEXT,
    created_at     DATETIME    NOT NULL DEFAULT (UTC_TIMESTAMP()),
    updated_at     DATETIME    NOT NULL DEFAULT (UTC_TIMESTAMP()) ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_bookings_lab       (lab_id),
    INDEX idx_bookings_requester (requester_id),
    INDEX idx_bookings_status    (status),
    INDEX idx_bookings_time      (start_time, end_time),
    CONSTRAINT fk_bookings_lab       FOREIGN KEY (lab_id)        REFERENCES labs(id),
    CONSTRAINT fk_bookings_requester FOREIGN KEY (requester_id)  REFERENCES users(id),
    CONSTRAINT fk_bookings_decided   FOREIGN KEY (decided_by_id) REFERENCES users(id),
    CONSTRAINT chk_bookings_time     CHECK (end_time > start_time),
    CONSTRAINT chk_bookings_status   CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED','COMPLETED')),
    CONSTRAINT chk_bookings_capacity CHECK (attendee_count > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Borrow requests

CREATE TABLE borrow_requests (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    requester_id       BIGINT       NOT NULL,
    purpose            VARCHAR(500) NOT NULL,
    expected_return_at DATETIME     NOT NULL,
    checked_out_at     DATETIME,
    actual_return_at   DATETIME,
    processed_by_id    BIGINT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    decision_note      TEXT,
    created_at         DATETIME     NOT NULL DEFAULT (UTC_TIMESTAMP()),
    updated_at         DATETIME     NOT NULL DEFAULT (UTC_TIMESTAMP()) ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_borrow_requester (requester_id),
    INDEX idx_borrow_status    (status),
    INDEX idx_borrow_due       (expected_return_at),
    CONSTRAINT fk_borrow_requester  FOREIGN KEY (requester_id)    REFERENCES users(id),
    CONSTRAINT fk_borrow_processor  FOREIGN KEY (processed_by_id) REFERENCES users(id),
    CONSTRAINT chk_borrow_status    CHECK (status IN ('PENDING','APPROVED','REJECTED','ACTIVE','RETURNED','CANCELLED','OVERDUE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE borrow_items (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    borrow_request_id     BIGINT      NOT NULL,
    equipment_id          BIGINT      NOT NULL,
    condition_at_checkout VARCHAR(20),
    condition_at_return   VARCHAR(20),
    notes                 TEXT,

    PRIMARY KEY (id),
    INDEX idx_borrow_items_request   (borrow_request_id),
    INDEX idx_borrow_items_equipment (equipment_id),
    CONSTRAINT fk_borrow_items_request   FOREIGN KEY (borrow_request_id) REFERENCES borrow_requests(id),
    CONSTRAINT fk_borrow_items_equipment FOREIGN KEY (equipment_id)      REFERENCES equipment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Maintenance tickets

CREATE TABLE maintenance_tickets (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    equipment_id     BIGINT      NOT NULL,
    reporter_id      BIGINT      NOT NULL,
    description      TEXT        NOT NULL,
    priority         VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    assignee_id      BIGINT,
    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    opened_at        DATETIME    NOT NULL DEFAULT (UTC_TIMESTAMP()),
    updated_at       DATETIME    NOT NULL DEFAULT (UTC_TIMESTAMP()) ON UPDATE CURRENT_TIMESTAMP,
    resolved_at      DATETIME,
    resolution_notes TEXT,
    repair_cost      DECIMAL(10, 2),

    PRIMARY KEY (id),
    INDEX idx_maintenance_equipment (equipment_id),
    INDEX idx_maintenance_status    (status),
    INDEX idx_maintenance_priority  (priority),
    CONSTRAINT fk_maintenance_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id),
    CONSTRAINT fk_maintenance_reporter  FOREIGN KEY (reporter_id)  REFERENCES users(id),
    CONSTRAINT fk_maintenance_assignee  FOREIGN KEY (assignee_id)  REFERENCES users(id),
    CONSTRAINT chk_maintenance_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    CONSTRAINT chk_maintenance_status   CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Audit log
-- Never stores passwords, tokens, or sensitive secrets.

CREATE TABLE audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    actor_id    BIGINT,                   -- nullable: system actions
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(60),
    target_id   BIGINT,
    details     TEXT,
    timestamp   DATETIME     NOT NULL DEFAULT (UTC_TIMESTAMP()),

    PRIMARY KEY (id),
    INDEX idx_audit_actor     (actor_id),
    INDEX idx_audit_action    (action),
    INDEX idx_audit_target    (target_type, target_id),
    INDEX idx_audit_timestamp (timestamp)
    -- No FK on actor_id: audit entries must persist even if user is deleted/disabled.
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
