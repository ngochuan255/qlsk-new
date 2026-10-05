package com.qlsk.config;

import com.qlsk.entity.*;
import com.qlsk.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init-sample-data:false}")
    private boolean initSampleData;

    @Override
    public void run(String... args) {
        try {
            // 1. Roles (cần thiết cho hệ thống phân quyền)
            Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ADMIN").build()));
            Role roleOrganizer = roleRepository.findByName("ROLE_ORGANIZER")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ORGANIZER").build()));
            Role roleAttendee = roleRepository.findByName("ROLE_ATTENDEE")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_ATTENDEE").build()));

            // 2. Admin: Reset hoặc khởi tạo tài khoản admin
            User adminUser = userRepository.findByUsername("admin").orElse(null);
            if (adminUser == null) {
                adminUser = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("123456"))
                        .fullName("Quản Trị Viên Hệ Thống")
                        .email("admin@qlsk.vn")
                        .phone("0901234567")
                        .role(roleAdmin)
                        .isActive(true)
                        .build();
                userRepository.save(adminUser);
                log.info("-> Đã khởi tạo tài khoản Admin ban đầu: admin / 123456");
            } else {
                adminUser.setPassword(passwordEncoder.encode("123456"));
                adminUser.setIsActive(true);
                userRepository.save(adminUser);
                log.info("-> Đã reset lại mật khẩu tài khoản Admin thành 123456");
            }

            // 3. Dữ liệu mẫu (chỉ khởi tạo nếu app.init-sample-data=true trong application.properties)
            if (initSampleData) {
                log.info("Cấu hình app.init-sample-data=true: Khởi tạo tài khoản mẫu và sự kiện mẫu...");

                // Chỉ tạo danh mục mẫu nếu hệ thống chưa có bất kỳ danh mục nào
                Category catTech = categoryRepository.findByName("Công nghệ & AI")
                        .or(() -> categoryRepository.findAll().stream().findFirst())
                        .orElse(null);
                if (catTech == null) {
                    catTech = categoryRepository.save(Category.builder()
                            .name("Công nghệ & AI")
                            .description("Các hội thảo, triển lãm và diễn đàn về công nghệ thông tin và AI")
                            .build());

                    categoryRepository.save(Category.builder()
                            .name("Giáo dục & Kỹ năng")
                            .description("Các khóa đào tạo, workshop phát triển kỹ năng mềm")
                            .build());

                    categoryRepository.save(Category.builder()
                            .name("Văn hóa & Nghệ thuật")
                            .description("Các chương trình âm nhạc, trình diễn nghệ thuật")
                            .build());
                }

                String defaultPassword = passwordEncoder.encode("123456");

                User organizer = userRepository.findByUsername("organizer1").orElse(null);
                if (organizer == null) {
                    organizer = User.builder()
                            .username("organizer1")
                            .password(defaultPassword)
                            .fullName("Ban Tổ Chức CLB Công Nghệ")
                            .email("btc.tech@qlsk.vn")
                            .phone("0912345678")
                            .role(roleOrganizer)
                            .isActive(true)
                            .build();
                    organizer = userRepository.save(organizer);
                    log.info("-> Đã tạo tài khoản BTC mẫu: organizer1 / 123456");
                }

                User attendee = userRepository.findByUsername("attendee1").orElse(null);
                if (attendee == null) {
                    attendee = User.builder()
                            .username("attendee1")
                            .password(defaultPassword)
                            .fullName("Nguyễn Văn Anh")
                            .email("vananh@gmail.com")
                            .phone("0987654321")
                            .role(roleAttendee)
                            .isActive(true)
                            .build();
                    userRepository.save(attendee);
                    log.info("-> Đã tạo tài khoản Người tham gia mẫu: attendee1 / 123456");
                }

                User adminUser2 = userRepository.findByUsername("admin").orElse(null);
                if (eventRepository.count() == 0 && organizer != null && adminUser2 != null && catTech != null) {
                    Event sampleEvent = Event.builder()
                            .title("Hội Thảo Trí Tuệ Nhân Tạo & Chuyển Đổi Số 2026")
                            .description("Sự kiện thường niên quy tụ các chuyên gia hàng đầu về AI, Cloud và chuyển đổi số trong giáo dục và doanh nghiệp.")
                            .category(catTech)
                            .organizer(organizer)
                            .startTime(LocalDateTime.now().plusDays(5).withHour(9).withMinute(0))
                            .endTime(LocalDateTime.now().plusDays(5).withHour(17).withMinute(0))
                            .location("Trung tâm Hội nghị Quốc tế, 11 Lê Hồng Phong, Hà Nội")
                            .maxAttendees(50)
                            .estimatedBudget(new BigDecimal("25000000.00"))
                            .status(EventStatus.OPEN_FOR_REGISTRATION)
                            .hasSeatMap(true)
                            .seatRows(5)
                            .seatCols(10)
                            .approvedBy(adminUser2)
                            .approvedAt(LocalDateTime.now())
                            .build();

                    Event savedEvent = eventRepository.save(sampleEvent);

                    for (int r = 1; r <= 5; r++) {
                        char rowChar = (char) ('A' + (r - 1));
                        for (int c = 1; c <= 10; c++) {
                            Seat seat = Seat.builder()
                                    .event(savedEvent)
                                    .seatCode(rowChar + String.valueOf(c))
                                    .seatRow(r)
                                    .seatCol(c)
                                    .seatType(r <= 2 ? "VIP" : "STANDARD")
                                    .isBooked(false)
                                    .build();
                            seatRepository.save(seat);
                        }
                    }
                    log.info("-> Đã khởi tạo sự kiện mẫu và ma trận 50 ghế ngồi.");
                }
            }
            
            // Tự động fix lỗi font chữ do schema.sql bị nạp sai encoding
            userRepository.findByUsername("admin").ifPresent(u -> {
                u.setFullName("Quản Trị Viên Hệ Thống");
                userRepository.save(u);
            });
            userRepository.findByUsername("organizer1").ifPresent(u -> {
                u.setFullName("Ban Tổ Chức CLB Công Nghệ");
                userRepository.save(u);
            });
            userRepository.findByUsername("attendee1").ifPresent(u -> {
                u.setFullName("Nguyễn Văn Anh");
                userRepository.save(u);
            });
            
            categoryRepository.findById(1L).ifPresent(c -> {
                c.setName("Công nghệ & AI");
                c.setDescription("Các hội thảo, triển lãm và diễn đàn về công nghệ thông tin và trí tuệ nhân tạo");
                categoryRepository.save(c);
            });
            categoryRepository.findById(2L).ifPresent(c -> {
                c.setName("Giáo dục & Kỹ năng");
                c.setDescription("Các khóa đào tạo, workshop phát triển kỹ năng mềm và hướng nghiệp");
                categoryRepository.save(c);
            });
            categoryRepository.findById(3L).ifPresent(c -> {
                c.setName("Văn hóa & Nghệ thuật");
                c.setDescription("Các chương trình âm nhạc, trình diễn nghệ thuật và giao lưu văn hóa");
                categoryRepository.save(c);
            });
            categoryRepository.findById(4L).ifPresent(c -> {
                c.setName("Thể thao & Sức khỏe");
                c.setDescription("Các giải đấu thể thao, ngày hội chạy bộ và sức khỏe cộng đồng");
                categoryRepository.save(c);
            });
            log.info("-> Đã tự động fix lỗi font chữ tiếng Việt (Mojibake) trong Database.");

        } catch (Exception e) {
            log.error("Lỗi trong quá trình khởi tạo dữ liệu ban đầu: {}", e.getMessage(), e);
        }
    }
}
