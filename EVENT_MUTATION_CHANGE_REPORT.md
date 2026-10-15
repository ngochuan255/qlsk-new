# Cập nhật sửa và xóa sự kiện — 15/10/2026

## Quy tắc đã áp dụng

- Bỏ ô số người tham gia tối đa khỏi form sửa của ban tổ chức và admin; payload sửa không gửi trường này. API từ chối yêu cầu cố đổi `maxAttendees`.
- Chỉ sửa/xóa nếu thời gian bắt đầu còn ở tương lai, trạng thái không phải ONGOING/COMPLETED, và sự kiện chưa từng có đăng ký. Đăng ký đã hủy vẫn khóa sửa/xóa.
- Kiểm tra quy tắc tại backend và giao diện; nút bị khóa được làm mờ và có lý do. API kiểm tra lại khi xử lý yêu cầu, kể cả khi màn hình đã mở từ trước.
- PATCH trạng thái và phê duyệt cũng kiểm tra điều kiện; các chuyển trạng thái tự động theo thời gian giữ nguyên.
- Ban tổ chức chỉ được sửa/xóa/đổi trạng thái sự kiện của mình; admin vẫn chịu điều kiện thời gian và đăng ký.
- Kiểm tra cả mốc thời gian hiện có trước khi sửa; không thể đổi ngày của một sự kiện đã bắt đầu để vượt điều kiện. Ngày mới phải ở tương lai và kết thúc phải sau bắt đầu.

## Bảo toàn điều kiện khi dữ liệu thay đổi

Thêm cột `events.has_registration_history` để giữ dấu đã có người đăng ký, kể cả khi tài khoản người tham dự bị xóa cùng các vé. Với dữ liệu vé cũ, thao tác xóa tài khoản đánh dấu lịch sử trước khi xóa bản ghi. Xóa tài khoản ban tổ chức cũng phải tuân thủ điều kiện khi xóa sự kiện do tài khoản đó tổ chức; nếu sự kiện bị khóa, giao dịch xóa tài khoản bị từ chối và rollback.

Đăng ký, sửa, xóa, đổi trạng thái và phê duyệt dùng khóa bản ghi sự kiện. Đăng ký lấy khóa trước các truy vấn thông thường. Xóa sự kiện refresh bản ghi đang khóa để tránh dùng dữ liệu cũ trong giao dịch xóa tài khoản. Chưa thực hiện load test hoặc kiểm thử cạnh tranh bằng nhiều transaction MySQL.

## Bằng chứng kiểm tra

- TDD trước thay đổi: 26 test sửa/xóa có 22 ca thất bại đúng hành vi còn thiếu. Trước khi xử lý đường vòng: 4/4 ca mới thất bại.
- Sau thay đổi, `EventMutationIntegrationTest`: **30/30 đạt**, bao gồm hai vai trò, sửa/xóa hợp lệ, thời gian đã bắt đầu, trạng thái đang diễn ra/kết thúc, đăng ký CONFIRMED/CANCELLED, bảo vệ sức chứa, ngày giờ, metadata UI, xóa tài khoản và API trạng thái/phê duyệt.
- JavaScript handler thực của hai trang: **8/8 kịch bản sửa/xóa đạt**; kiểm tra hồi quy tạo sự kiện **6/6 đạt**. Dùng Node VM với DOM mô phỏng.
- Frontend tĩnh: **30 khối JavaScript, 125 tham chiếu nội bộ, 0 lỗi**. `git diff --check` không phát hiện lỗi whitespace.
- Toàn bộ Maven suite: **116 test, 100 đạt, 16 thất bại, 0 error, 0 skipped**. Log: `event-mutation-full.log`; XML chi tiết: `target/codex-event-verification/surefire-reports/`.
- Đóng gói JAR thành công: `event-mutation-package.log`.
- Đã thay JAR và khởi động lại ứng dụng local tại `http://127.0.0.1:8080`, PID 3168 tại lúc xác minh. Bản JAR cũ lưu tại `target/qlsk-system-1.0.0.before-event-mutation-20261015-230909.jar`.
- HTTP trên ứng dụng mới dùng cấu hình MySQL local: hai trang trả 200, không còn input `edit-event-max`; script quyền thao tác trả 200. API công khai trả 7 sự kiện, đủ metadata mới: 4 có thể sửa/xóa và 3 bị khóa.

Không có browser khả dụng qua công cụ điều khiển, nên chưa kiểm tra bố cục và thao tác trong trình duyệt thật. Các HTTP kiểm tra live chỉ đọc dữ liệu; thay đổi nghiệp vụ được kiểm tra tích hợp trên H2.

## 16 test còn thất bại ngoài phạm vi lần này

`EventCapacityKnownIssuesTest` — 11 ca:

- `enabledSeatMapRequiresDimensionsWithinTheFormLimits`: 8 bộ hàng/cột thiếu, không dương hoặc vượt giới hạn giao diện.
- `seatMapRegistrationMustSelectASeat`: backend chưa bắt buộc ghế khi đăng ký sự kiện có sơ đồ.
- `editingDimensionsMustKeepMetadataAndPhysicalSeatsConsistent`: sửa kích thước sơ đồ chưa tái tạo ghế thực tế.
- `cancelledSeatMustBeBookableByAnotherAttendee`: ràng buộc ghế của vé đã hủy ngăn đặt lại.

`KnownIssuesTest` — 5 ca:

- `organizerCannotChargeAnotherOrganizersEvent`.
- `readingTicketsMustPreserveCompletedAttendanceHistory`.
- `organizerCannotCheckInAnotherOrganizersAttendee`.
- `organizerCannotReadAnotherOrganizersReport`.
- `fullEventMustAdvanceToOngoing`.

Không làm yếu assertion để che lỗi. Hai test cũ được cập nhật theo yêu cầu mới: xóa sự kiện đã có vé phải bảo toàn dữ liệu; tăng sức chứa sự kiện FULL đã có người đăng ký phải bị từ chối.

## Chạy lại

```powershell
mvn -o -B -ntp "-Dmaven.repo.local=C:\Users\Lenovo\.m2\repository" "-Dmaven.clean.failOnError=false" "-Dtest=EventMutationIntegrationTest" clean test
node test-event-mutations.cjs
node test-event-capacity.cjs
node test-frontend.cjs
```

Để chạy toàn bộ suite, bỏ tham số `-Dtest=...`; hiện sẽ trả exit code 1 do 16 ca đã liệt kê. Lượt xác minh thực tế dùng bản sao POM tạm chỉ đổi thư mục build sang `target/codex-event-verification` để tránh IDE can thiệp file class và JAR đang chạy bị khóa. Không thay đổi `pom.xml` của dự án.

Các chỉnh sửa đã có trước trong `css/seating.css` và `event-detail.html` được giữ nguyên.
