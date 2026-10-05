package com.qlsk.entity;

public enum EventStatus {
    DRAFT,                  // Nháp
    APPROVED,               // Đã duyệt (chờ mở đăng ký)
    OPEN_FOR_REGISTRATION,  // Đang mở đăng ký
    FULL,                   // Đã đủ chỗ
    ONGOING,                // Đang diễn ra
    COMPLETED,              // Đã kết thúc
    CANCELLED               // Đã hủy
}
