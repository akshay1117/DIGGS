-- ============================================================
--  Digital Grievance Portal — Database Setup
--  Schema aligned with ER Diagram (3NF Normalized)
-- ============================================================

CREATE DATABASE IF NOT EXISTS diggs_db;
USE diggs_db;

-- ============================================================
-- 1. DEPARTMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS departments (
    department_id   VARCHAR(50)  PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL,
    department_code VARCHAR(20)  UNIQUE NOT NULL,
    description     TEXT,
    sla_days        INT          DEFAULT 7,
    created_date    DATETIME     DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_dept_code (department_code)
);

-- ============================================================
-- 2. USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    user_id       VARCHAR(50)  PRIMARY KEY,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    phone_number  VARCHAR(15)  UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role          ENUM('CITIZEN', 'OFFICER', 'AUTHORITY', 'AUDITOR') NOT NULL,
    address       VARCHAR(200),
    city          VARCHAR(50),
    state         VARCHAR(50),
    pincode       VARCHAR(20),
    status        ENUM('ACTIVE', 'INACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE',
    create_date   DATETIME     DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_email  (email),
    INDEX idx_phone  (phone_number),
    INDEX idx_role   (role)
);

-- ============================================================
-- 3. GRIEVANCES
-- ============================================================
CREATE TABLE IF NOT EXISTS grievances (
    grievance_id      VARCHAR(50)  PRIMARY KEY,
    user_id           VARCHAR(50)  NOT NULL,
    department_id     VARCHAR(50)  NOT NULL,
    case_number       VARCHAR(50)  UNIQUE NOT NULL,
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    priority          ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    status            ENUM(
                          'SUBMITTED',
                          'UNDER_REVIEW',
                          'RESOLVED',
                          'ESCALATED',
                          'REJECTED',
                          'CLOSED'
                      ) DEFAULT 'SUBMITTED',
    date_submitted    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    due_date          DATETIME,
    date_resolved     DATETIME,
    resolution_notes  TEXT,
    satisfaction_rating INT        CHECK (satisfaction_rating BETWEEN 1 AND 5),

    FOREIGN KEY (user_id)       REFERENCES users(user_id),
    FOREIGN KEY (department_id) REFERENCES departments(department_id),

    INDEX idx_status        (status),
    INDEX idx_user          (user_id),
    INDEX idx_department    (department_id),
    INDEX idx_case_number   (case_number),
    INDEX idx_due_date      (due_date)
);

-- ============================================================
-- 4. ESCALATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS escalations (
    escalation_id     VARCHAR(50) PRIMARY KEY,
    grievance_id      VARCHAR(50) NOT NULL,
    escalation_level  INT         DEFAULT 1,
    escalation_date   DATETIME    DEFAULT CURRENT_TIMESTAMP,
    escalation_status ENUM('PENDING', 'IN_PROGRESS', 'RESOLVED') DEFAULT 'PENDING',
    reason            TEXT,

    FOREIGN KEY (grievance_id) REFERENCES grievances(grievance_id),

    INDEX idx_grievance  (grievance_id),
    INDEX idx_esc_status (escalation_status)
);

-- ============================================================
-- 5. EVIDENCE
-- ============================================================
CREATE TABLE IF NOT EXISTS evidence (
    evidence_id   VARCHAR(50)  PRIMARY KEY,
    file_name     VARCHAR(255) NOT NULL,
    file_type     VARCHAR(50),
    file_path     VARCHAR(500) NOT NULL,
    uploaded_date DATETIME     DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_file_type (file_type)
);

-- ============================================================
-- 6. WORKFLOWLOGS
-- ============================================================
CREATE TABLE IF NOT EXISTS workflowlogs (
    log_id          VARCHAR(50) PRIMARY KEY,
    grievance_id    VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50),
    new_status      VARCHAR(50) NOT NULL,
    remarks         TEXT,
    change_date     DATETIME    DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (grievance_id) REFERENCES grievances(grievance_id),

    INDEX idx_grievance   (grievance_id),
    INDEX idx_change_date (change_date)
);

-- ============================================================
-- 7. AUDITLOGS
-- ============================================================
CREATE TABLE IF NOT EXISTS auditlogs (
    audit_id         VARCHAR(50)  PRIMARY KEY,
    user_id          VARCHAR(50)  NOT NULL,
    entity_name      VARCHAR(100),
    record_id        VARCHAR(50),
    action_type      VARCHAR(100) NOT NULL,
    action_timestamp DATETIME     DEFAULT CURRENT_TIMESTAMP,
    ip_address       VARCHAR(45),

    FOREIGN KEY (user_id) REFERENCES users(user_id),

    INDEX idx_user      (user_id),
    INDEX idx_record    (record_id),
    INDEX idx_timestamp (action_timestamp)
);

-- ============================================================
-- 8. HANDLES  (junction — User handles Grievance)
-- ============================================================
CREATE TABLE IF NOT EXISTS handles (
    user_id      VARCHAR(50) NOT NULL,
    grievance_id VARCHAR(50) NOT NULL,

    PRIMARY KEY (user_id, grievance_id),
    FOREIGN KEY (user_id)      REFERENCES users(user_id),
    FOREIGN KEY (grievance_id) REFERENCES grievances(grievance_id),

    INDEX idx_handles_user      (user_id),
    INDEX idx_handles_grievance (grievance_id)
);

-- ============================================================
-- 9. ATTACHMENT  (junction — Grievance ↔ Evidence)
-- ============================================================
CREATE TABLE IF NOT EXISTS attachment (
    grievance_id VARCHAR(50) NOT NULL,
    evidence_id  VARCHAR(50) NOT NULL,

    PRIMARY KEY (grievance_id, evidence_id),
    FOREIGN KEY (grievance_id) REFERENCES grievances(grievance_id),
    FOREIGN KEY (evidence_id)  REFERENCES evidence(evidence_id),

    INDEX idx_attach_grievance (grievance_id),
    INDEX idx_attach_evidence  (evidence_id)
);

-- ============================================================
-- NOTIFICATIONS  (retained from original — not in ER diagram)
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    notification_id      VARCHAR(50) PRIMARY KEY,
    user_id              VARCHAR(50) NOT NULL,
    title                VARCHAR(200) NOT NULL,
    message              TEXT,
    type                 ENUM(
                             'GRIEVANCE_UPDATE',
                             'SLA_WARNING',
                             'ESCALATION',
                             'ASSIGNMENT',
                             'RESOLUTION',
                             'SYSTEM_ALERT'
                         ),
    related_grievance_id VARCHAR(50),
    timestamp            DATETIME    DEFAULT CURRENT_TIMESTAMP,
    is_read              BOOLEAN     DEFAULT FALSE,

    FOREIGN KEY (user_id)              REFERENCES users(user_id),
    FOREIGN KEY (related_grievance_id) REFERENCES grievances(grievance_id),

    INDEX idx_notif_user   (user_id),
    INDEX idx_notif_read   (is_read)
);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Departments
INSERT INTO departments (department_id, department_name, department_code, description, sla_days)
VALUES
    ('DEPT001', 'Public Works',    'PW',  'Roads, infrastructure, utilities', 7),
    ('DEPT002', 'Internal Audit',  'IA',  'Audit and compliance',             14),
    ('DEPT003', 'Administration',  'ADM', 'General administration',           10),
    ('DEPT004', 'Sanitation',      'SAN', 'Waste management and cleaning',    5),
    ('DEPT005', 'Electricity',     'ELE', 'Power supply and grids',           3),
    ('DEPT006', 'Water Supply',    'WAT', 'Pipes, leakage, and water',        4),
    ('DEPT007', 'Transport',       'TRP', 'Public transit and mobility',      10);

-- Users  (password = '123' — replace with bcrypt hashes in production)
INSERT INTO users (user_id, first_name, last_name, email, phone_number, password, role)
VALUES
    ('CIT001', 'Test',   'Citizen',   'citizen@test.com',   '1234567890', '123', 'CITIZEN'),
    ('OFF001', 'Test',   'Officer',   'officer@test.com',   '0987654321', '123', 'OFFICER'),
    ('AUT001', 'Test',   'Authority', 'authority@test.com', '1122334455', '123', 'AUTHORITY'),
    ('AUD001', 'Test',   'Auditor',   'auditor@test.com',   '5566778899', '123', 'AUDITOR');

-- Grievance
INSERT INTO grievances (
    grievance_id, user_id, department_id, case_number,
    title, description, priority, status, due_date
)
VALUES (
    'GRV001', 'CIT001', 'DEPT001', 'CASE-2026-0001',
    'Pothole on Main Street',
    'Large pothole causing traffic issues near Main Street junction.',
    'HIGH', 'SUBMITTED',
    DATE_ADD(NOW(), INTERVAL 7 DAY)
);

-- Workflow log for the initial submission
INSERT INTO workflowlogs (log_id, grievance_id, previous_status, new_status, remarks)
VALUES ('WFL001', 'GRV001', NULL, 'SUBMITTED', 'Grievance submitted by citizen.');

-- Officer handles grievance
INSERT INTO handles (user_id, grievance_id)
VALUES ('OFF001', 'GRV001');

-- Audit log
INSERT INTO auditlogs (audit_id, user_id, entity_name, record_id, action_type, ip_address)
VALUES ('AUD_LOG001', 'CIT001', 'grievances', 'GRV001', 'CREATE', '127.0.0.1');