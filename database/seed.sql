-- ============================================================
-- LabHub - Seed Data
-- Run after schema.sql.
-- Passwords below are BCrypt hashes of:
--   admin123   -> for admin account
--   manager123 -> for lab_manager account
--   lecturer123-> for lecturer accounts
--   student123 -> for student accounts
-- CHANGE ALL PASSWORDS before any real use.
-- ============================================================

USE labhub;

-- Roles

INSERT INTO roles (name) VALUES
    ('ADMIN'),
    ('LAB_MANAGER'),
    ('LECTURER'),
    ('STUDENT');

-- Users
-- BCrypt cost factor 12 (at.favre.lib:bcrypt:0.10.2)

INSERT INTO users (username, email, full_name, phone_number, password_hash, is_active) VALUES
-- Admin
('admin',
 'admin@university.edu.vn',
 'System Administrator',
 '+84-28-1234-5678',
 '$2a$12$d8omIbZ7LGlGpSpWPXNTPuNesXH/ugZ7vViflDjk6itFW0gfqLiOC',   -- admin123
 1),

-- Lab Manager
('dr.nam',
 'nam.tran@university.edu.vn',
 'Dr. Trần Minh Nam',
 '+84-909-111-222',
 '$2a$12$ZtpqeSWG7SkbB071vW/zZO0P/J52Tz0XC1.OGW/DwS6K5pafb57Se',   -- manager123
 1),

-- Lecturers
('prof.linh',
 'linh.nguyen@university.edu.vn',
 'Prof. Nguyễn Thu Linh',
 '+84-908-333-444',
 '$2a$12$bcswG9VDixDNnu458Ewx0e6WE04u.Ucw7N32psoVwvKNAkZxmZgbu',   -- lecturer123
 1),
('mr.hieu',
 'hieu.le@university.edu.vn',
 'Mr. Lê Văn Hiếu',
 '+84-907-555-666',
 '$2a$12$bcswG9VDixDNnu458Ewx0e6WE04u.Ucw7N32psoVwvKNAkZxmZgbu',   -- lecturer123
 1),

-- Students
('quan.tran',
 'quan.tran@student.university.edu.vn',
 'Trần Minh Quân',
 '+84-901-777-888',
 '$2a$12$HnmYAGEig6D9U7JSbWLW.uURMR8Y3JrISn4X4H3Ua1n9vXA9tV1U6',   -- student123
 1),
('anh.nguyen',
 'anh.nguyen@student.university.edu.vn',
 'Nguyễn Thị Ánh',
 '+84-902-999-000',
 '$2a$12$HnmYAGEig6D9U7JSbWLW.uURMR8Y3JrISn4X4H3Ua1n9vXA9tV1U6',   -- student123
 1),
('duc.pham',
 'duc.pham@student.university.edu.vn',
 'Phạm Tiến Đức',
 '+84-903-123-456',
 '$2a$12$HnmYAGEig6D9U7JSbWLW.uURMR8Y3JrISn4X4H3Ua1n9vXA9tV1U6',   -- student123
 1);

-- Assign roles

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE (u.username = 'admin'      AND r.name = 'ADMIN')
   OR (u.username = 'admin'      AND r.name = 'LAB_MANAGER')
   OR (u.username = 'dr.nam'     AND r.name = 'LAB_MANAGER')
   OR (u.username = 'prof.linh'  AND r.name = 'LECTURER')
   OR (u.username = 'mr.hieu'    AND r.name = 'LECTURER')
   OR (u.username = 'quan.tran'  AND r.name = 'STUDENT')
   OR (u.username = 'anh.nguyen' AND r.name = 'STUDENT')
   OR (u.username = 'duc.pham'   AND r.name = 'STUDENT');

-- Laboratories

INSERT INTO labs (lab_code, name, building, floor, capacity, room_type, description, status) VALUES
('CS-LAB-A',
 'Computer Lab A',
 'Engineering Building',
 2,
 40,
 'Computer Lab',
 'General-purpose computing lab with Dell workstations. Used for programming courses and project development.',
 'ACTIVE'),

('NET-LAB-B',
 'Networking Lab',
 'Engineering Building',
 3,
 24,
 'Networking Lab',
 'Equipped with Cisco routers, switches, and patch panels. Used for network architecture and protocols courses.',
 'ACTIVE'),

('EMB-LAB-C',
 'Embedded Systems Lab',
 'Technology Center',
 1,
 20,
 'Embedded Systems Lab',
 'Arduino kits, Raspberry Pi stations, oscilloscopes, and logic analyzers for embedded and IoT coursework.',
 'ACTIVE'),

('SE-LAB-D',
 'Software Engineering Lab',
 'Engineering Building',
 2,
 30,
 'Computer Lab',
 'Dedicated lab for software engineering teams. Equipped with high-spec workstations and dual-monitor setups.',
 'ACTIVE'),

('PROJ-LAB-E',
 'Project Lab E',
 'Innovation Hub',
 1,
 16,
 'Project Lab',
 'Open project space for capstone teams. Flexible layout with modular workbenches.',
 'INACTIVE');

-- Equipment categories

INSERT INTO equipment_categories (name, description) VALUES
('Workstation',       'Desktop computers and workstations'),
('Networking',        'Network infrastructure equipment: routers, switches, patch panels'),
('Microcontroller',   'Arduino, Raspberry Pi, and similar embedded platforms'),
('Test & Measurement','Oscilloscopes, multimeters, signal generators, logic analyzers'),
('Audio/Visual',      'Projectors, monitors, cameras, and display equipment'),
('Storage',           'External drives, NAS units, and data storage devices'),
('Peripheral',        'Keyboards, mice, webcams, and input devices'),
('Power',             'UPS units, power strips, and power management equipment');

-- Equipment

INSERT INTO equipment
    (asset_code, name, category_id, manufacturer, model, serial_number, lab_id,
     acquisition_date, purchase_cost, condition_state, status, notes)
VALUES
-- Computer Lab A - workstations
('DELL-WS-001', 'Dell OptiPlex 7090', 1, 'Dell', 'OptiPlex 7090', 'SN-DELL-7090-001',
 (SELECT id FROM labs WHERE lab_code = 'CS-LAB-A'),
 '2022-09-01', 18500000.00, 'GOOD', 'AVAILABLE', 'i7-11700, 32GB RAM, 512GB SSD'),

('DELL-WS-002', 'Dell OptiPlex 7090', 1, 'Dell', 'OptiPlex 7090', 'SN-DELL-7090-002',
 (SELECT id FROM labs WHERE lab_code = 'CS-LAB-A'),
 '2022-09-01', 18500000.00, 'GOOD', 'AVAILABLE', NULL),

('DELL-WS-003', 'Dell OptiPlex 7090', 1, 'Dell', 'OptiPlex 7090', 'SN-DELL-7090-003',
 (SELECT id FROM labs WHERE lab_code = 'CS-LAB-A'),
 '2022-09-01', 18500000.00, 'FAIR', 'MAINTENANCE', 'Fan making abnormal noise - ticket open'),

('HP-WS-001', 'HP EliteDesk 800 G8', 1, 'HP', 'EliteDesk 800 G8', 'SN-HP-ELT-001',
 (SELECT id FROM labs WHERE lab_code = 'SE-LAB-D'),
 '2023-01-15', 21000000.00, 'EXCELLENT', 'AVAILABLE', 'i9-11900, 64GB RAM, 1TB NVMe'),

('HP-WS-002', 'HP EliteDesk 800 G8', 1, 'HP', 'EliteDesk 800 G8', 'SN-HP-ELT-002',
 (SELECT id FROM labs WHERE lab_code = 'SE-LAB-D'),
 '2023-01-15', 21000000.00, 'EXCELLENT', 'BORROWED', NULL),

-- Networking Lab - Cisco equipment
('CISCO-RT-001', 'Cisco ISR 4321 Router', 2, 'Cisco', 'ISR 4321', 'SN-CISCO-4321-001',
 (SELECT id FROM labs WHERE lab_code = 'NET-LAB-B'),
 '2021-06-10', 35000000.00, 'GOOD', 'AVAILABLE', 'IOS 16.09.08, dual GE WAN'),

('CISCO-SW-001', 'Cisco Catalyst 2960-X Switch', 2, 'Cisco', 'Catalyst 2960-X', 'SN-CISCO-2960-001',
 (SELECT id FROM labs WHERE lab_code = 'NET-LAB-B'),
 '2021-06-10', 28000000.00, 'GOOD', 'AVAILABLE', '24-port managed switch'),

('CISCO-SW-002', 'Cisco Catalyst 2960-X Switch', 2, 'Cisco', 'Catalyst 2960-X', 'SN-CISCO-2960-002',
 (SELECT id FROM labs WHERE lab_code = 'NET-LAB-B'),
 '2021-06-10', 28000000.00, 'FAIR', 'AVAILABLE', 'Port 12 intermittent - monitor'),

-- Embedded Systems Lab
('ARD-KIT-001', 'Arduino Mega 2560 Kit', 3, 'Arduino', 'Mega 2560', NULL,
 (SELECT id FROM labs WHERE lab_code = 'EMB-LAB-C'),
 '2023-03-01', 1200000.00, 'EXCELLENT', 'AVAILABLE', 'Kit includes sensors, breadboard, jumper wires'),

('ARD-KIT-002', 'Arduino Mega 2560 Kit', 3, 'Arduino', 'Mega 2560', NULL,
 (SELECT id FROM labs WHERE lab_code = 'EMB-LAB-C'),
 '2023-03-01', 1200000.00, 'GOOD', 'BORROWED', NULL),

('RPI-KIT-001', 'Raspberry Pi 4B Kit (4GB)', 3, 'Raspberry Pi Foundation', 'Pi 4 Model B', 'SN-RPI-4B-001',
 (SELECT id FROM labs WHERE lab_code = 'EMB-LAB-C'),
 '2023-03-01', 2500000.00, 'EXCELLENT', 'AVAILABLE', '4GB RAM, 32GB SD, official case'),

('RPI-KIT-002', 'Raspberry Pi 4B Kit (4GB)', 3, 'Raspberry Pi Foundation', 'Pi 4 Model B', 'SN-RPI-4B-002',
 (SELECT id FROM labs WHERE lab_code = 'EMB-LAB-C'),
 '2023-03-01', 2500000.00, 'GOOD', 'AVAILABLE', NULL),

('OSC-001', 'Rigol DS1054Z Oscilloscope', 4, 'Rigol', 'DS1054Z', 'SN-RIGOL-DS1054-001',
 (SELECT id FROM labs WHERE lab_code = 'EMB-LAB-C'),
 '2022-02-14', 8900000.00, 'GOOD', 'AVAILABLE', '50MHz, 4-channel, 12Mpts memory'),

('OSC-002', 'Rigol DS1054Z Oscilloscope', 4, 'Rigol', 'DS1054Z', 'SN-RIGOL-DS1054-002',
 (SELECT id FROM labs WHERE lab_code = 'EMB-LAB-C'),
 '2022-02-14', 8900000.00, 'POOR', 'MAINTENANCE', 'Probe channel 3 damaged. Sent for repair.'),

-- Audio/Visual
('PROJ-001', 'Epson EB-X51 Projector', 5, 'Epson', 'EB-X51', 'SN-EPSON-X51-001',
 (SELECT id FROM labs WHERE lab_code = 'CS-LAB-A'),
 '2021-08-20', 12000000.00, 'GOOD', 'AVAILABLE', '3600 lumens, HDMI/VGA, ceiling-mounted'),

('PROJ-002', 'Epson EB-X51 Projector', 5, 'Epson', 'EB-X51', 'SN-EPSON-X51-002',
 (SELECT id FROM labs WHERE lab_code = 'NET-LAB-B'),
 '2021-08-20', 12000000.00, 'FAIR', 'AVAILABLE', 'Lamp hours: 2800 (replace soon)');

-- Bookings
-- Mix of past, today, upcoming; multiple statuses

INSERT INTO bookings
    (lab_id, requester_id, start_time, end_time, purpose, attendee_count, status, decided_by_id, decided_at, decision_note)
VALUES
-- Approved booking - yesterday
((SELECT id FROM labs WHERE lab_code = 'CS-LAB-A'),
 (SELECT id FROM users WHERE username = 'prof.linh'),
 DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 DAY), '%Y-%m-%d 08:00:00'),
 DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 DAY), '%Y-%m-%d 11:00:00'),
 'Introduction to Python Programming - Lab Session',
 35, 'COMPLETED',
 (SELECT id FROM users WHERE username = 'dr.nam'),
 DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 3 DAY), '%Y-%m-%d 09:00:00'),
 NULL),

-- Approved booking - today morning
((SELECT id FROM labs WHERE lab_code = 'SE-LAB-D'),
 (SELECT id FROM users WHERE username = 'mr.hieu'),
 DATE_FORMAT(CURDATE(), '%Y-%m-%d 08:30:00'),
 DATE_FORMAT(CURDATE(), '%Y-%m-%d 11:30:00'),
 'Software Architecture - Group Project Review',
 25, 'APPROVED',
 (SELECT id FROM users WHERE username = 'dr.nam'),
 DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 2 DAY), '%Y-%m-%d 14:00:00'),
 NULL),

-- Approved booking - today afternoon
((SELECT id FROM labs WHERE lab_code = 'NET-LAB-B'),
 (SELECT id FROM users WHERE username = 'prof.linh'),
 DATE_FORMAT(CURDATE(), '%Y-%m-%d 13:00:00'),
 DATE_FORMAT(CURDATE(), '%Y-%m-%d 16:00:00'),
 'Network Protocols Lab - TCP/IP Deep Dive',
 20, 'APPROVED',
 (SELECT id FROM users WHERE username = 'dr.nam'),
 DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 DAY), '%Y-%m-%d 10:00:00'),
 NULL),

-- Pending - awaiting approval
((SELECT id FROM labs WHERE lab_code = 'EMB-LAB-C'),
 (SELECT id FROM users WHERE username = 'quan.tran'),
 DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '%Y-%m-%d 09:00:00'),
 DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '%Y-%m-%d 12:00:00'),
 'IoT Sensor Project - Capstone Lab Work',
 4, 'PENDING', NULL, NULL, NULL),

-- Pending - lectuerer request
((SELECT id FROM labs WHERE lab_code = 'CS-LAB-A'),
 (SELECT id FROM users WHERE username = 'mr.hieu'),
 DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '%Y-%m-%d 14:00:00'),
 DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '%Y-%m-%d 17:00:00'),
 'Algorithm Design - Problem Set Session',
 38, 'PENDING', NULL, NULL, NULL),

-- Rejected
((SELECT id FROM labs WHERE lab_code = 'SE-LAB-D'),
 (SELECT id FROM users WHERE username = 'anh.nguyen'),
 DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d 08:00:00'),
 DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '%Y-%m-%d 09:00:00'),
 'Personal study session',
 1, 'REJECTED',
 (SELECT id FROM users WHERE username = 'dr.nam'),
 DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'),
 'SE Lab D is reserved for group project work (5+ members). Please use the library for individual study.');

-- Borrow requests

-- Active loan - Arduino kit borrowed by student
INSERT INTO borrow_requests
    (requester_id, purpose, expected_return_at, checked_out_at, status, processed_by_id)
VALUES
((SELECT id FROM users WHERE username = 'quan.tran'),
 'IoT weather station project - capstone assignment',
 DATE_ADD(NOW(), INTERVAL 5 DAY),
 DATE_SUB(NOW(), INTERVAL 2 DAY),
 'ACTIVE',
 (SELECT id FROM users WHERE username = 'dr.nam'));

INSERT INTO borrow_items (borrow_request_id, equipment_id, condition_at_checkout)
SELECT
    (SELECT id FROM borrow_requests ORDER BY id DESC LIMIT 1),
    (SELECT id FROM equipment WHERE asset_code = 'ARD-KIT-002'),
    'GOOD';

-- Pending loan request
INSERT INTO borrow_requests (requester_id, purpose, expected_return_at, status)
VALUES
((SELECT id FROM users WHERE username = 'duc.pham'),
 'Networking course lab assignment - packet analysis demo',
 DATE_ADD(NOW(), INTERVAL 3 DAY),
 'PENDING');

INSERT INTO borrow_items (borrow_request_id, equipment_id)
SELECT
    (SELECT id FROM borrow_requests ORDER BY id DESC LIMIT 1),
    (SELECT id FROM equipment WHERE asset_code = 'CISCO-RT-001');

-- Overdue loan - HP workstation
INSERT INTO borrow_requests
    (requester_id, purpose, expected_return_at, checked_out_at, status, processed_by_id)
VALUES
((SELECT id FROM users WHERE username = 'anh.nguyen'),
 'Machine learning model training - group project',
 DATE_SUB(NOW(), INTERVAL 2 DAY),   -- past due!
 DATE_SUB(NOW(), INTERVAL 7 DAY),
 'OVERDUE',
 (SELECT id FROM users WHERE username = 'dr.nam'));

INSERT INTO borrow_items (borrow_request_id, equipment_id, condition_at_checkout)
SELECT
    (SELECT id FROM borrow_requests ORDER BY id DESC LIMIT 1),
    (SELECT id FROM equipment WHERE asset_code = 'HP-WS-002'),
    'EXCELLENT';

-- Maintenance tickets

INSERT INTO maintenance_tickets
    (equipment_id, reporter_id, description, priority, assignee_id, status)
VALUES
-- Open ticket - oscilloscope probe
((SELECT id FROM equipment WHERE asset_code = 'OSC-002'),
 (SELECT id FROM users WHERE username = 'prof.linh'),
 'Channel 3 probe input damaged - no signal detected on CH3. Equipment pulled from service.',
 'HIGH',
 (SELECT id FROM users WHERE username = 'dr.nam'),
 'IN_PROGRESS'),

-- Open ticket - PC fan
((SELECT id FROM equipment WHERE asset_code = 'DELL-WS-003'),
 (SELECT id FROM users WHERE username = 'quan.tran'),
 'CPU fan making grinding noise. Temperature readings normal but fan failure risk.',
 'MEDIUM',
 NULL,
 'OPEN'),

-- Resolved ticket - projector lamp
((SELECT id FROM equipment WHERE asset_code = 'PROJ-001'),
 (SELECT id FROM users WHERE username = 'mr.hieu'),
 'Projector lamp blinking - lamp warning threshold exceeded.',
 'LOW',
 (SELECT id FROM users WHERE username = 'dr.nam'),
 'CLOSED');

-- Update resolved ticket timestamps
UPDATE maintenance_tickets
SET resolved_at     = DATE_SUB(NOW(), INTERVAL 10 DAY),
    resolution_notes = 'Lamp replaced with compatible Epson ELPLP96 unit. Reset lamp counter.',
    repair_cost      = 850000.00
WHERE equipment_id = (SELECT id FROM equipment WHERE asset_code = 'PROJ-001')
  AND status = 'CLOSED';

-- Audit log - representative entries

INSERT INTO audit_logs (actor_id, action, target_type, target_id, details) VALUES
((SELECT id FROM users WHERE username = 'admin'),
 'USER_CREATED', 'USER', (SELECT id FROM users WHERE username = 'dr.nam'),
 'Created lab manager account for Dr. Trần Minh Nam'),

((SELECT id FROM users WHERE username = 'dr.nam'),
 'BOOKING_APPROVED', 'BOOKING', 2,
 'Approved booking for Software Architecture group project review'),

((SELECT id FROM users WHERE username = 'dr.nam'),
 'BOOKING_REJECTED', 'BOOKING', 6,
 'Rejected personal study booking - policy: SE Lab D for group projects only'),

((SELECT id FROM users WHERE username = 'dr.nam'),
 'BORROW_APPROVED', 'BORROW_REQUEST', 1,
 'Approved Arduino kit loan for capstone IoT project'),

((SELECT id FROM users WHERE username = 'prof.linh'),
 'MAINTENANCE_CREATED', 'EQUIPMENT',
 (SELECT id FROM equipment WHERE asset_code = 'OSC-002'),
 'Filed maintenance ticket: CH3 probe input damaged on DS1054Z oscilloscope');
