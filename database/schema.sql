-- ====================================================================
-- HỆ THỐNG QUẢN LÝ TỔ CHỨC SỰ KIỆN VÀ KIỂM SOÁT THAM DỰ (QLSK)
-- Database Script: MySQL 8.0+
-- ====================================================================

CREATE DATABASE IF NOT EXISTS qlsk_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE qlsk_db;

-- 1. Bảng Vai trò (Roles)
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Bảng Người dùng (Users)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    role_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Bảng Danh mục sự kiện (Categories)
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Bảng Sự kiện (Events)
-- Trạng thái: DRAFT (Nháp), APPROVED (Đã duyệt), OPEN_FOR_REGISTRATION (Mở đăng ký), FULL (Đủ chỗ), ONGOING (Đang diễn ra), COMPLETED (Kết thúc), CANCELLED (Hủy)
CREATE TABLE IF NOT EXISTS events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT,
    category_id BIGINT NOT NULL,
    organizer_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    location VARCHAR(255) NOT NULL,
    max_attendees INT NOT NULL DEFAULT 50,
    estimated_budget DECIMAL(15, 2) DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    banner_url VARCHAR(500),
    has_seat_map BOOLEAN DEFAULT FALSE,
    seat_rows INT DEFAULT 0,
    seat_cols INT DEFAULT 0,
    approved_by BIGINT,
    approved_at DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_events_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users(id),
    CONSTRAINT fk_events_approved_by FOREIGN KEY (approved_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Bảng Chỗ ngồi sự kiện (Seats)
CREATE TABLE IF NOT EXISTS seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    seat_code VARCHAR(20) NOT NULL, -- Ví dụ: A1, A2, B1, VIP1...
    seat_row INT NOT NULL,
    seat_col INT NOT NULL,
    seat_type VARCHAR(20) DEFAULT 'STANDARD', -- VIP, STANDARD, v.v.
    is_booked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_seats_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    UNIQUE KEY uk_event_seat (event_id, seat_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Bảng Đăng ký sự kiện (Registrations)
-- Trạng thái: CONFIRMED, CANCELLED
CREATE TABLE IF NOT EXISTS registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    registration_code VARCHAR(50) NOT NULL UNIQUE,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    seat_id BIGINT,
    qr_code_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cancelled_at DATETIME,
    CONSTRAINT fk_registrations_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_registrations_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_registrations_seat FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Bảng Điểm danh / Check-in bằng QR (Check-ins)
CREATE TABLE IF NOT EXISTS check_ins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    registration_id BIGINT NOT NULL UNIQUE,
    event_id BIGINT NOT NULL,
    check_in_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    check_in_by BIGINT NOT NULL, -- Ban tổ chức thực hiện quét mã
    note VARCHAR(255),
    CONSTRAINT fk_checkins_registration FOREIGN KEY (registration_id) REFERENCES registrations(id) ON DELETE CASCADE,
    CONSTRAINT fk_checkins_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_checkins_user FOREIGN KEY (check_in_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Bảng Quản lý Kinh phí (Expenses)
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    amount DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    expense_date DATE NOT NULL,
    note VARCHAR(255),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expenses_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. Bảng Đánh giá sự kiện (Feedbacks)
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_feedbacks_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_feedbacks_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_event_user_feedback (event_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ====================================================================
-- DỮ LIỆU KHỞI TẠO BAN ĐẦU (SEED DATA)
-- Mật khẩu mặc định của các tài khoản mẫu: 123456 (BCrypt hash)
-- BCrypt của "123456" = $2a$10$wT3WfI9vJ0o3q6k8t4WdK.5qE5D1DvhE/Vq5Zf8aMsh8WvJbUu0uK
-- ====================================================================

-- 1. Insert Roles
INSERT INTO roles (id, name) VALUES 
(1, 'ROLE_ADMIN'),
(2, 'ROLE_ORGANIZER'),
(3, 'ROLE_ATTENDEE')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 2. Insert Users (Admin, Ban tổ chức, Người tham gia)
INSERT INTO users (id, username, password, full_name, email, phone, role_id, is_active) VALUES
(1, 'admin', '$2a$10$wT3WfI9vJ0o3q6k8t4WdK.5qE5D1DvhE/Vq5Zf8aMsh8WvJbUu0uK', 'Quản Trị Viên Hệ Thống', 'admin@qlsk.vn', '0901234567', 1, TRUE),
(2, 'organizer1', '$2a$10$wT3WfI9vJ0o3q6k8t4WdK.5qE5D1DvhE/Vq5Zf8aMsh8WvJbUu0uK', 'Ban Tổ Chức CLB Công Nghệ', 'btc.tech@qlsk.vn', '0912345678', 2, TRUE),
(3, 'attendee1', '$2a$10$wT3WfI9vJ0o3q6k8t4WdK.5qE5D1DvhE/Vq5Zf8aMsh8WvJbUu0uK', 'Nguyễn Văn Anh', 'vananh@gmail.com', '0987654321', 3, TRUE)
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- 3. Insert Danh mục mẫu
INSERT INTO categories (id, name, description) VALUES
(1, 'Công nghệ & AI', 'Các hội thảo, triển lãm và diễn đàn về công nghệ thông tin và trí tuệ nhân tạo'),
(2, 'Giáo dục & Kỹ năng', 'Các khóa đào tạo, workshop phát triển kỹ năng mềm và hướng nghiệp'),
(3, 'Văn hóa & Nghệ thuật', 'Các chương trình âm nhạc, trình diễn nghệ thuật và giao lưu văn hóa'),
(4, 'Thể thao & Sức khỏe', 'Các giải đấu thể thao, ngày hội chạy bộ và sức khỏe cộng đồng')
ON DUPLICATE KEY UPDATE name=VALUES(name);
