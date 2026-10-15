# Kiểm tra tạo sự kiện và quan hệ sức chứa / số ghế

Ngày kiểm tra: 15/10/2026. Không thay đổi mã nghiệp vụ hoặc giao diện.

> Đây là kết quả tại thời điểm kiểm tra trước khi thay đổi quy tắc sửa/xóa. Các thay đổi tiếp theo và kết quả hiện tại được ghi tại [EVENT_MUTATION_CHANGE_REPORT.md](EVENT_MUTATION_CHANGE_REPORT.md).

## Hành vi hiện tại đã xác minh

| Cấu hình khi tạo | Kết quả lưu |
| --- | --- |
| Không bật sơ đồ ghế, tối đa 17 người | `maxAttendees = 17`, không sinh bản ghi ghế |
| Bật sơ đồ 2 hàng × 3 ghế, nhập tối đa 4, 6 hoặc 100 người | Luôn sinh 6 ghế và ghi đè `maxAttendees = 6` |
| Tạo thành công | Trạng thái `DRAFT`, chủ sở hữu là tài khoản ban tổ chức gửi yêu cầu |
| Sau khi admin duyệt | `OPEN_FOR_REGISTRATION`, có thể đăng ký |
| Số vé CONFIRMED bằng sức chứa | Chuyển `FULL`, không nhận lượt tiếp theo trong kiểm thử tuần tự |
| Hủy một vé | Giảm số người đăng ký, tăng chỗ còn lại, trả ghế về trống, chuyển `FULL` về mở đăng ký |

`remainingSeats = max(0, maxAttendees - số đăng ký CONFIRMED)`. Đây là số suất còn lại tính theo vé, không phải truy vấn đếm ghế trống.

Hai giao diện `organizer/event-form.html` và modal tạo tại `organizer/events.html` đều không tự đồng bộ ô tối đa khi bật sơ đồ ghế. Ví dụ: giao diện gửi tối đa 100 và sơ đồ 2 × 3; backend lưu tối đa 6. Kiểm thử JavaScript thực thi handler gốc đã xác nhận điều này.

## Các lỗi đã tái hiện

16 ca kiểm thử thất bại thuộc 8 nhóm bên dưới. Đây là test yêu cầu tính nhất quán, giữ nguyên assertion để thể hiện lỗi hiện tại.

| Nhóm | Ca lỗi | Tái hiện và ảnh hưởng |
| --- | ---: | --- |
| Không kiểm tra kích thước sơ đồ tại API | 8 | Bật sơ đồ với hàng/cột thiếu, 0 hoặc âm vẫn trả 200 và không tạo ghế. 27 hàng hoặc 31 ghế/hàng cũng được chấp nhận, vượt giới hạn 26/30 trên giao diện. |
| Đăng ký thiếu ghế | 1 | Sự kiện có sơ đồ nhưng POST đăng ký không có `seatId` vẫn thành công. Vé chiếm suất nhưng không chiếm ghế; chỗ còn lại có thể lệch số ghế trống. Giao diện chi tiết có kiểm tra chọn ghế nhưng backend không bắt buộc. |
| Sửa tối đa độc lập với tổng ghế | 2 | Sự kiện có 6 ghế vẫn cho sửa tối đa thành 1 hoặc 10. Trường hợp 1 đóng đăng ký sớm; trường hợp 10 cho phép vượt số ghế nếu gửi đăng ký thiếu ghế. |
| Sửa hàng/cột không cập nhật ghế thật | 1 | Đổi từ 2 × 3 sang 3 × 4: metadata thành 12 vị trí nhưng bảng ghế vẫn chỉ có 6 bản ghi. |
| Giảm sức chứa dưới số người đã đăng ký | 1 | Đã có 2 vé CONFIRMED, vẫn sửa tối đa xuống 1. `remainingSeats` bị chặn về 0 nên không biểu lộ mức vượt sức chứa. |
| Tăng sức chứa không mở lại sự kiện FULL | 1 | Sự kiện đủ 1/1 người, sửa tối đa thành 2: vẫn giữ `FULL`, không nhận người thứ hai. |
| Bật sơ đồ sau khi đã có đăng ký | 1 | Đã có 2 vé, bật sơ đồ 1 × 1 vẫn thành công và hạ tối đa xuống 1. Vé cũ vẫn không có ghế. |
| Ghế hủy không thể đặt lại | 1 | Đặt ghế, hủy vé rồi tài khoản khác đặt lại: HTTP 400 do unique constraint `registrations(seat_id)`. Quan hệ `@OneToOne` giữ liên kết ghế trên cả vé CANCELLED. Đã xác nhận trên schema H2 do Hibernate tạo; chưa xác minh schema MySQL hiện tại. |

## Kết quả kiểm thử

- Lượt chạy hồi quy + kiểm tra mới: **78 test, 62 đạt, 16 thất bại, 0 error, 0 skipped**.
  - `ApplicationIntegrationTest`: 46/46 đạt.
  - `EventCapacityIntegrationTest`: 16/16 đạt.
  - `EventCapacityKnownIssuesTest`: 16/16 thất bại để tái hiện các nhóm lỗi trên.
- Sáu kịch bản JavaScript trên hai giao diện: **6/6 đạt** theo hành vi hiện tại; dùng Node VM và DOM mô phỏng, không phải browser thật.
- Kiểm tra tĩnh frontend: **29 khối JavaScript, 123 tham chiếu nội bộ, 0 lỗi**.
- Sau khi cải thiện assertion cập nhật để chấp nhận cả phương án từ chối thay đổi không hợp lệ hoặc giữ bất biến, chạy lại riêng hai lớp mới: **32 test, 16 đạt, 16 thất bại, 0 error, 0 skipped**. Log: `capacity-focused-verification.log` và Surefire.

Lượt Maven có `clean` đã gặp JAR trong `target` bị khóa. Chạy lại với `maven.clean.failOnError=false`; Maven biên dịch lại 56 file nguồn và 5 file test. Không đóng ứng dụng đang sử dụng JAR.

Không chạy `KnownIssuesTest` cũ trong lượt 78 test này. Không kiểm thử MySQL, giao diện bằng trình duyệt thật hoặc đăng ký đồng thời. Kết quả tuần tự chưa chứng minh việc chống vượt chỗ khi nhiều request đến cùng lúc.

## Hướng thống nhất logic

Theo hành vi tạo hiện tại, khi có sơ đồ ghế, nên giữ bất biến `maxAttendees = số ghế thực tế = seatRows × seatCols`. Giao diện nên hiển thị giá trị tự tính và backend phải kiểm tra độc lập. Không bật sơ đồ thì giới hạn theo số vé CONFIRMED.

Ưu tiên xử lý: kiểm tra hàng/cột tại API; bắt buộc ghế cho đăng ký có sơ đồ; kiểm soát sửa sức chứa/sơ đồ khi đã có vé; đồng bộ trạng thái FULL; sửa mô hình liên kết ghế để giữ lịch sử vé hủy nhưng cho phép tái đặt ghế. Cần thêm kiểm thử đồng thời trước khi kết luận chống trùng ghế/vượt sức chứa.

## Chạy lại

```powershell
mvn -o -B -ntp "-Dmaven.repo.local=C:\Users\Lenovo\.m2\repository" "-Dtest=ApplicationIntegrationTest,EventCapacityIntegrationTest,EventCapacityKnownIssuesTest" test
node test-event-capacity.cjs
node test-frontend.cjs
```

Maven hiện trả exit code 1 vì 16 assertion tái hiện lỗi, không phải kết quả toàn bộ đạt.

File mới: `src/test/java/com/qlsk/EventCapacityIntegrationTest.java`, `src/test/java/com/qlsk/EventCapacityKnownIssuesTest.java`, `test-event-capacity.cjs`, báo cáo này. Không thay đổi hai file giao diện/CSS vốn đã có chỉnh sửa trước lúc kiểm tra.
