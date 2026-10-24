-- ============================================================================
-- SMART CAMPUS PARKING - DATABASE MIGRATION V1
-- Chuẩn hóa đúng 10 thực thể theo Sơ đồ EER và Sơ đồ lớp chính thức
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. NGUOIDUNG (users) - Khớp Bảng 1 trong tài liệu
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT', -- 'ADMIN', 'STUDENT'
    active BOOLEAN NOT NULL DEFAULT TRUE,        -- Khớp thuộc tính active: boolean trong class User
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. SINHVIEN (students) - Khớp Bảng 2 trong tài liệu (Không có PIN/LicensePlate)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        user_id BIGINT UNIQUE,
                                        student_code VARCHAR(20) NOT NULL UNIQUE,     -- Mã sinh viên (MSSV)
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(15),                     -- Số điện thoại
    faculty VARCHAR(100),                          -- Khoa
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. VIDIENTU (wallets)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallets (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       student_id BIGINT NOT NULL UNIQUE,
                                       balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. GIAODICHVI (wallet_transactions)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wallet_transactions (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   wallet_id BIGINT NOT NULL,
                                                   tx_code VARCHAR(50) NOT NULL UNIQUE,
    tx_type VARCHAR(30) NOT NULL,                -- 'TOPUP_VIETQR', 'PARKING_FEE', 'PACKAGE_PURCHASE'
    amount DECIMAL(12, 2) NOT NULL,
    balance_before DECIMAL(12, 2) NOT NULL,
    balance_after DECIMAL(12, 2) NOT NULL,
    idempotency_key VARCHAR(64) UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. THESINHVIEN (cards)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cards (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     student_id BIGINT NOT NULL,
                                     card_uid VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'LOCKED'
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. LICHSUTHE (card_status_history)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS card_status_history (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   card_id BIGINT NOT NULL,
                                                   old_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    changed_by VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. GOICUOC (card_packages)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS card_packages (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             card_id BIGINT NOT NULL,
                                             package_name VARCHAR(100) NOT NULL,
    package_type VARCHAR(20) NOT NULL,           -- 'WEEKLY', 'MONTHLY', 'TRIP_COUNT'
    price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_trips INT DEFAULT NULL,
    remaining_trips INT DEFAULT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_package_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. CONGKIEMSOAT (parking_gates)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS parking_gates (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             gate_code VARCHAR(20) NOT NULL UNIQUE,       -- GATE_IN_01, GATE_OUT_01
    gate_name VARCHAR(100) NOT NULL,
    gate_type VARCHAR(10) NOT NULL,              -- 'ENTRY', 'EXIT'
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 9. BANGGIA (parking_rates)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS parking_rates (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             rate_code VARCHAR(30) NOT NULL UNIQUE,
    rate_type VARCHAR(30) NOT NULL,              -- 'PER_ENTRY', 'BLOCK_4H', 'PER_HOUR'
    price_per_entry DECIMAL(12, 2) DEFAULT 3000.00,
    price_block_4h DECIMAL(12, 2) DEFAULT 3000.00,
    price_extra_per_hour DECIMAL(12, 2) DEFAULT 1000.00,
    price_per_hour DECIMAL(12, 2) DEFAULT 2000.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 10. PHIENGUIXE (parking_sessions)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS parking_sessions (
                                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                session_code VARCHAR(50) NOT NULL UNIQUE,
    card_uid VARCHAR(64) NOT NULL,
    card_id BIGINT,
    entry_gate_id BIGINT,
    exit_gate_id BIGINT,
    rate_id BIGINT,
    package_id BIGINT,
    wallet_tx_id BIGINT,

    -- Đối soát ảnh ANPR
    entry_plate_number VARCHAR(20) NOT NULL,
    exit_plate_number VARCHAR(20),
    entry_image_url VARCHAR(255),
    exit_image_url VARCHAR(255),
    plate_mismatch BOOLEAN NOT NULL DEFAULT FALSE,

    -- Thời gian & Chi phí
    entry_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    exit_time TIMESTAMP NULL,
    duration_minutes INT DEFAULT NULL,
    fee DECIMAL(12, 2) DEFAULT 0.00,
    payment_method VARCHAR(20),

    -- Trạng thái
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    verified_by VARCHAR(30) NOT NULL DEFAULT 'NONE',
    idempotency_key VARCHAR(64) UNIQUE,

    CONSTRAINT fk_session_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE SET NULL,
    CONSTRAINT fk_session_entry_gate FOREIGN KEY (entry_gate_id) REFERENCES parking_gates(id) ON DELETE SET NULL,
    CONSTRAINT fk_session_exit_gate FOREIGN KEY (exit_gate_id) REFERENCES parking_gates(id) ON DELETE SET NULL,
    CONSTRAINT fk_session_rate FOREIGN KEY (rate_id) REFERENCES parking_rates(id) ON DELETE SET NULL,
    CONSTRAINT fk_session_pkg FOREIGN KEY (package_id) REFERENCES card_packages(id) ON DELETE SET NULL,
    CONSTRAINT fk_session_tx FOREIGN KEY (wallet_tx_id) REFERENCES wallet_transactions(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;