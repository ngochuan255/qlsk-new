# Báo cáo kiểm thử QLSK — 15/10/2026

Ứng dụng đang chạy nền tại **http://127.0.0.1:8080/**, kết nối MySQL local theo cấu hình dự án. Tài khoản admin đã kiểm tra đăng nhập: `admin` / `123456`.

## Kết quả

| Hạng mục | Kết quả |
| --- | --- |
| Biên dịch sạch Java 17, 56 file nguồn | Đạt |
| Bộ test tích hợp trên H2 trong RAM | 54 test: 46 đạt, 8 thất bại, 0 lỗi thực thi, 0 bỏ qua |
| Cùng bộ test trên MySQL, database tạm riêng | 54 test: 46 đạt, 8 thất bại, 0 lỗi thực thi, 0 bỏ qua |
| HTTP trên ứng dụng local và database hiện có | 39/39 đạt |
| Cú pháp JavaScript riêng và inline | 26/26 khối đạt |
| Liên kết trang và tài nguyên nội bộ tĩnh | 114/114 tồn tại |
| Đóng gói JAR để chạy local | Đạt với `-DskipTests`, sau khi đã lưu kết quả test thất bại |

`mvn verify` hiện **thất bại vì 8 test phát hiện lỗi**, không thể kết luận toàn bộ dự án đã đạt kiểm thử. Không sửa mã nghiệp vụ trong đợt kiểm thử này.

Database test MySQL `qlsk_codex_test_20261015150120` đã được tạo riêng và xóa sau kiểm thử. Mỗi test dùng transaction rollback. Kiểm tra HTTP trên database hiện có sử dụng các API đọc và đăng nhập; không gọi API xem vé cá nhân vì endpoint này có lỗi xóa lịch sử nêu dưới đây.

## Các lỗi được tái hiện bằng test

| Mức độ | Test trong `KnownIssuesTest` | Kết quả thực tế và ảnh hưởng |
| --- | --- | --- |
| Cao | `organizerCannotEditAnotherOrganizersEvent` | BTC khác sửa được sự kiện qua PUT `/api/organizer/events/{id}`; HTTP 200 thay vì 403. `EventService.java:186`. |
| Cao | `organizerCannotDeleteAnotherOrganizersEvent` | BTC khác xóa được sự kiện cùng dữ liệu phụ thuộc; HTTP 200 thay vì 403. `EventService.java:262`. |
| Cao | `organizerCannotChangeAnotherOrganizersEventStatus` | BTC khác đổi được trạng thái sự kiện; HTTP 200 thay vì 403. `EventService.java:109`. |
| Cao | `organizerCannotCheckInAnotherOrganizersAttendee` | BTC khác check-in được người tham dự của sự kiện không thuộc mình; HTTP 200 thay vì 403. `CheckInService.java:22`. |
| Cao | `organizerCannotReadAnotherOrganizersReport` | BTC khác đọc được báo cáo tài chính; HTTP 200 thay vì 403. `ReportService.java:59`. |
| Cao | `organizerCannotChargeAnotherOrganizersEvent` | BTC khác thêm được chi phí cho sự kiện không thuộc mình; HTTP 200 thay vì 403. `ExpenseService.java:27`. |
| Cao | `readingTicketsMustPreserveCompletedAttendanceHistory` | GET `/api/registrations/my-tickets` xóa registration và check-in khi sự kiện COMPLETED, làm mất dữ liệu báo cáo và điều kiện gửi đánh giá. `RegistrationService.java:166`. |
| Trung bình | `fullEventMustAdvanceToOngoing` | Sự kiện FULL đã tới giờ bắt đầu vẫn FULL; tác vụ định kỳ chỉ xử lý OPEN_FOR_REGISTRATION và ONGOING. `EventService.java:166`. |

Các test trên mô tả hành vi mong đợi và được giữ nguyên để phát hiện hồi quy khi sửa lỗi. Không đổi assertion theo hành vi sai để làm test xanh.

Ngoài 8 lỗi trên, log khởi động xác nhận `DataInitializer` luôn reset mật khẩu admin thành `123456`, bật lại admin và ghi lại một số tên tài khoản/danh mục, kể cả khi `app.init-sample-data=false` (`DataInitializer.java:56`). Đây là hành vi hiện tại của mã nguồn, cần xem xét trước khi triển khai dùng thật.

## Phạm vi đã kiểm tra

- Đăng nhập username/email, JWT thật và truy cập profile, sai mật khẩu, validation đăng ký, trùng tài khoản.
- Chặn anonymous/attendee ở API cần quyền; kiểm tra quyền sở hữu sự kiện giữa hai BTC khác nhau.
- Tạo sự kiện, sinh ma trận ghế, duyệt mở đăng ký, tìm kiếm/lọc, lấy danh mục và ghế.
- Đặt vé, PNG QR, chống đăng ký trùng/đặt ghế đã có người, chặn sự kiện nháp, hủy vé, giải phóng ghế, FULL và mở lại đăng ký.
- Check-in hợp lệ, QR vé đã hủy, check-in trùng, chặn hủy vé đã check-in.
- Điều kiện đánh giá sau sự kiện và có check-in, chống đánh giá trùng.
- Chi phí, validation số tiền, báo cáo tham dự/tài chính/đánh giá, dashboard và biểu đồ admin.
- Cập nhật profile, đổi mật khẩu, CRUD người dùng/danh mục, chống admin tự khóa, xóa sự kiện với dữ liệu phụ thuộc.
- Tải toàn bộ trang HTML/CSS/JS local, kiểm tra liên kết nội bộ, từ chối upload rỗng hoặc không phải ảnh.

Chưa xác minh thao tác giao diện trong trình duyệt thật, responsive, thư viện CDN, camera/quét QR từ thiết bị, tải ảnh hợp lệ, kiểm thử tải và tranh chấp đặt ghế đồng thời. Công cụ trình duyệt không có browser khả dụng trong phiên này. MockMvc và kiểm tra HTTP không thay thế kiểm thử giao diện.

## Chạy lại

```powershell
# Toàn bộ test; hiện trả exit code 1 vì các lỗi đã nêu.
mvn test

# Riêng 46 test chức năng đang đạt.
mvn -Dtest=ApplicationIntegrationTest test

# Cú pháp JavaScript và tham chiếu tĩnh.
node test-frontend.cjs

# Kiểm tra ứng dụng local đang chạy, dùng admin mẫu.
powershell -NoProfile -File .\smoke-local.ps1

# Chạy local từ mã nguồn, cần MySQL.
mvn spring-boot:run
```

Maven trên máy này mặc định tìm kho `C:\.m2\repository`; các lệnh đã chạy dùng thêm `-Dmaven.repo.local=C:\Users\Lenovo\.m2\repository` để sử dụng kho thư viện đã cài. Nếu cổng 8080 đang được ứng dụng này dùng thì không khởi động thêm một bản nữa.

Trước đó, build tăng dần lấy phải `.class` cũ không hợp lệ và gây `ClassFormatError` ở `Category`. `mvn clean verify` đã biên dịch lại từ mã nguồn; bản JAR hiện tại khởi động thành công.

## Bằng chứng

- `verification.log`: kết quả H2.
- `verification-mysql.log`: kết quả MySQL và tên schema test riêng.
- `target/surefire-reports/`: báo cáo test JUnit gần nhất (MySQL).
- `target/local-smoke-results.json`: từng kiểm tra HTTP.
- `build-local.log`: log đóng gói bản chạy local.
- `target/local-app.log`, `target/local-app-error.log`: log server.
- `target/local-app.pid`: PID server nền (34024 tại thời điểm lập báo cáo).

Để dừng đúng server của phiên này: `Stop-Process -Id 34024`.
