# Hệ Thống Quản Lý Tổ Chức Sự Kiện & Kiểm Soát Tham Dự (QLSK)

Dự án ứng dụng web toàn diện phục vụ quản lý sự kiện từ khâu khởi tạo, phê duyệt, mở đăng ký, chọn sơ đồ ghế ngồi, tạo vé điện tử QR Code, trạm kiểm soát check-in camera thời gian thực, quản lý ngân sách kinh phí cho tới đánh giá và xuất báo cáo thống kê.

---

## 1. Công Nghệ Sử Dụng
- **Backend:** Java 17+, Spring Boot 3.2.5 (Spring Data JPA, Spring Security, Spring Web, Spring Validation)
- **Database:** MySQL 8.0+
- **Bảo mật & Phân quyền:** Spring Security + JWT Stateless Authentication (Role-based: ADMIN, ORGANIZER, ATTENDEE)
- **Mã QR Code:** Google ZXing Library (Tự động sinh mã vé QR Code PNG/Base64)
- **Frontend:** HTML5, CSS3 hiện đại, Vanilla JavaScript (Tích hợp responsive, thư viện quét camera `html5-qrcode`)
- **Build tool:** Maven (`pom.xml`)

---

## 2. Cấu Trúc Thư Mục Dự Án (Project Structure)

```text
d:\QLSK\
├── pom.xml                                    # File cấu hình thư viện Maven
├── README.md                                  # Tài liệu hướng dẫn sử dụng và kiến trúc
├── database\
│   └── schema.sql                             # Script khởi tạo Database MySQL và Seed Data
└── src\
    └── main\
        ├── java\com\qlsk\
        │   ├── QlskApplication.java           # Điểm khởi chạy ứng dụng Spring Boot
        │   ├── config\                        # Cấu hình hệ thống
        │   │   ├── SecurityConfig.java        # Phân quyền Spring Security theo Roles
        │   │   └── WebMvcConfig.java          # Cấu hình CORS & Static Resources
        │   ├── controller\                    # Lớp điều khiển REST API
        │   │   ├── AuthController.java        # Đăng ký & Đăng nhập
        │   │   ├── UserController.java        # Quản lý tài khoản (Admin) & Profile
        │   │   ├── CategoryController.java    # Quản lý danh mục sự kiện
        │   │   ├── EventController.java       # Quản lý sự kiện, sơ đồ ghế & duyệt sự kiện
        │   │   ├── RegistrationController.java# Đăng ký vé, chọn ghế & hủy vé
        │   │   ├── CheckInController.java     # Kiểm soát Check-in QR
        │   │   ├── ExpenseController.java     # Quản lý kinh phí sự kiện
        │   │   ├── FeedbackController.java    # Đánh giá & nhận xét sau sự kiện
        │   │   └── ReportController.java      # Báo cáo tham dự, tài chính & tổng quan
        │   ├── dto\                           # Đối tượng truyền dữ liệu (Request/Response)
        │   │   ├── AuthDtos.java
        │   │   ├── EventDtos.java
        │   │   ├── RegistrationDtos.java
        │   │   ├── CheckInDtos.java
        │   │   ├── ExpenseDtos.java
        │   │   ├── FeedbackDtos.java
        │   │   └── ReportDtos.java
        │   ├── entity\                        # Lớp thực thể JPA ánh xạ MySQL
        │   │   ├── EventStatus.java           # Trạng thái sự kiện (DRAFT, APPROVED, OPEN_FOR_REGISTRATION, FULL, ONGOING, COMPLETED, CANCELLED)
        │   │   ├── RegistrationStatus.java    # Trạng thái đăng ký (CONFIRMED, CANCELLED)
        │   │   ├── Role.java
        │   │   ├── User.java
        │   │   ├── Category.java
        │   │   ├── Event.java
        │   │   ├── Seat.java
        │   │   ├── Registration.java
        │   │   ├── CheckIn.java
        │   │   ├── Expense.java
        │   │   └── Feedback.java
        │   ├── exception\
        │   │   └── GlobalExceptionHandler.java# Bắt lỗi toàn cục trả về JSON rõ ràng
        │   ├── repository\                    # Tầng truy vấn dữ liệu JPA
        │   │   ├── RoleRepository.java
        │   │   ├── UserRepository.java
        │   │   ├── CategoryRepository.java
        │   │   ├── EventRepository.java
        │   │   ├── SeatRepository.java
        │   │   ├── RegistrationRepository.java
        │   │   ├── CheckInRepository.java
        │   │   ├── ExpenseRepository.java
        │   │   └── FeedbackRepository.java
        │   ├── security\                      # Xử lý JWT & Xác thực
        │   │   ├── CustomUserDetailsService.java
        │   │   ├── JwtAuthenticationFilter.java
        │   │   ├── JwtTokenProvider.java
        │   │   └── UserPrincipal.java
        │   └── service\                       # Tầng nghiệp vụ Logic (Business Logic)
        │       ├── AuthService.java
        │       ├── UserService.java
        │       ├── CategoryService.java
        │       ├── EventService.java
        │       ├── RegistrationService.java
        │       ├── CheckInService.java
        │       ├── QrCodeService.java
        │       ├── ExpenseService.java
        │       ├── FeedbackService.java
        │       └── ReportService.java
        └── resources\
            ├── application.properties         # File cấu hình kết nối DB & Port
            └── static\                        # Giao diện người dùng Frontend
                ├── css\
                │   ├── style.css              # Design system chung, responsive navbar, buttons
                │   ├── seating.css            # Giao diện sơ đồ chỗ ngồi tương tác
                │   └── dashboard.css          # Giao diện bảng điều khiển Admin / Organizer
                ├── js\
                │   ├── api.js                 # HTTP fetch wrapper tự động gắn JWT Token
                │   ├── auth.js                # Quản lý phiên đăng nhập & phân quyền menu
                │   └── seat-map.js            # Module sơ đồ ghế ngồi
                ├── index.html                 # Trang chủ khám phá & lọc sự kiện
                ├── login.html                 # Trang đăng nhập (có nút đăng nhập nhanh mẫu)
                ├── register.html              # Trang đăng ký người tham gia
                ├── event-detail.html          # Chi tiết sự kiện, chọn ghế ngồi, đánh giá
                ├── my-tickets.html            # Trang vé của tôi (hiển thị mã QR Code & hủy vé)
                ├── organizer\                 # Kênh dành cho Ban Tổ Chức
                │   ├── index.html             # Tổng quan hoạt động BTC
                │   ├── events.html            # Danh sách & chuyển đổi trạng thái sự kiện
                │   ├── event-form.html        # Tạo sự kiện mới, sinh sơ đồ ghế, lập dự toán
                │   ├── checkin.html           # Trạm quét Camera QR Check-in thời gian thực
                │   └── reports.html           # Báo cáo tham dự, thu chi ngân sách & đánh giá sao
                └── admin\                     # Kênh dành cho Quản Trị Viên
                    └── index.html             # Phê duyệt sự kiện, quản lý người dùng, danh mục & Dashboard
```

---

## 3. Danh Sách Tài Khoản Mẫu Mặc Định (Seed Data)
| Vai trò | Tên đăng nhập | Mật khẩu | Chức năng chính |
| :--- | :--- | :--- | :--- |
| **Quản Trị Viên (Admin)** | `admin` | `123456` | Duyệt sự kiện, quản lý tài khoản, danh mục, xem thống kê toàn hệ thống |
| **Ban Tổ Chức (Organizer)** | `organizer1` | `123456` | Tạo sự kiện, sinh sơ đồ ghế, quét QR Check-in, quản lý kinh phí & báo cáo |
| **Người Tham Gia (Attendee)** | `attendee1` | `123456` | Xem & tìm kiếm sự kiện, chọn chỗ ngồi trên sơ đồ, nhận vé QR, hủy vé, đánh giá sao |

---

## 4. Hướng Dẫn Cài Đặt & Chạy Dự Án

### Bước 1: Khởi tạo Cơ Sở Dữ Liệu MySQL
1. Mở MySQL Workbench hoặc chạy lệnh terminal MySQL:
   ```sql
   source d:/QLSK/database/schema.sql;
   ```
2. Kiểm tra thông tin kết nối trong `src/main/resources/application.properties`:
   - `spring.datasource.username=root`
   - `spring.datasource.password=123456` (sửa lại theo mật khẩu MySQL trên máy của bạn nếu khác)

### Bước 2: Biên Dịch & Khởi Chạy Ứng Dụng
Trong thư mục dự án `d:\QLSK`, mở terminal và chạy:
```bash
mvn spring-boot:run
```

### Bước 3: Truy Cập Ứng Dụng Trên Trình Duyệt
- **Trang chủ khách / Người tham gia:** [http://localhost:8080](http://localhost:8080)
- **Trang đăng nhập:** [http://localhost:8080/login.html](http://localhost:8080/login.html)
- **Kênh Ban Tổ Chức:** [http://localhost:8080/organizer/index.html](http://localhost:8080/organizer/index.html)
- **Trạm quét QR Check-in:** [http://localhost:8080/organizer/checkin.html](http://localhost:8080/organizer/checkin.html)
- **Bảng Quản Trị Admin:** [http://localhost:8080/admin/index.html](http://localhost:8080/admin/index.html)
