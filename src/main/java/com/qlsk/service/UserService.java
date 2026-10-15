package com.qlsk.service;

import com.qlsk.dto.AuthDtos.UserProfileDto;
import com.qlsk.entity.Event;
import com.qlsk.entity.Role;
import com.qlsk.entity.User;
import com.qlsk.repository.EventRepository;
import com.qlsk.repository.RoleRepository;
import com.qlsk.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    @PersistenceContext
    private final EntityManager entityManager;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));
        return toDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, String fullName, String phone) {
        return updateProfile(userId, fullName, phone, null, null);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, String fullName, String phone, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        if (phone != null) {
            user.setPhone(phone.trim());
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("Mật khẩu hiện tại không chính xác");
            }
            if (newPassword.trim().length() < 6) {
                throw new RuntimeException("Mật khẩu mới phải có ít nhất 6 ký tự");
            }
            user.setPassword(passwordEncoder.encode(newPassword.trim()));
        }

        return toDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserProfileDto toggleUserStatus(Long currentAdminId, Long userId) {
        if (currentAdminId != null && currentAdminId.equals(userId)) {
            throw new RuntimeException("Bạn không thể tự khóa tài khoản của chính mình đang đăng nhập!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        user.setIsActive(!user.getIsActive());
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserProfileDto changeUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ: " + roleName));

        user.setRole(role);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserProfileDto createUser(com.qlsk.dto.AuthDtos.AdminUserFormRequest req) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Tên đăng nhập không được để trống");
        }
        if (userRepository.existsByUsername(req.getUsername().trim())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trên hệ thống");
        }
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (userRepository.existsByEmail(req.getEmail().trim())) {
            throw new RuntimeException("Email đã được đăng ký trên hệ thống");
        }
        if (req.getPassword() == null || req.getPassword().trim().length() < 6) {
            throw new RuntimeException("Mật khẩu tối thiểu 6 ký tự");
        }

        String roleName = (req.getRole() != null && !req.getRole().isEmpty()) ? req.getRole() : "ROLE_ATTENDEE";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ: " + roleName));

        User user = User.builder()
                .username(req.getUsername().trim())
                .password(passwordEncoder.encode(req.getPassword().trim()))
                .fullName(req.getFullName() != null ? req.getFullName().trim() : req.getUsername().trim())
                .email(req.getEmail().trim().toLowerCase())
                .phone(req.getPhone() != null ? req.getPhone().trim() : null)
                .role(role)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .build();

        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserProfileDto updateUser(Long currentAdminId, Long userId, com.qlsk.dto.AuthDtos.AdminUserFormRequest req) {
        if (currentAdminId != null && currentAdminId.equals(userId) && Boolean.FALSE.equals(req.getIsActive())) {
            throw new RuntimeException("Bạn không thể tự khóa tài khoản của chính mình đang đăng nhập!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + userId));

        if (req.getEmail() != null && !req.getEmail().trim().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail().trim())) {
                throw new RuntimeException("Email đã tồn tại trên hệ thống");
            }
            user.setEmail(req.getEmail().trim().toLowerCase());
        }

        if (req.getFullName() != null && !req.getFullName().trim().isEmpty()) {
            user.setFullName(req.getFullName().trim());
        }

        if (req.getPhone() != null) {
            user.setPhone(req.getPhone().trim());
        }

        if (req.getRole() != null && !req.getRole().isEmpty()) {
            Role role = roleRepository.findByName(req.getRole())
                    .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ: " + req.getRole()));
            user.setRole(role);
        }

        if (req.getIsActive() != null) {
            user.setIsActive(req.getIsActive());
        }

        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            if (req.getPassword().trim().length() < 6) {
                throw new RuntimeException("Mật khẩu mới tối thiểu 6 ký tự");
            }
            user.setPassword(passwordEncoder.encode(req.getPassword().trim()));
        }

        return toDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long currentAdminId, Long targetUserId) {
        if (currentAdminId != null && currentAdminId.equals(targetUserId)) {
            throw new RuntimeException("Bạn không thể tự xóa tài khoản quản trị của chính mình!");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + targetUserId));

        // 1. Xác định ID admin thay thế nếu cần gán lại các dữ liệu phụ thuộc
        Long fallbackAdminId = currentAdminId;
        if (fallbackAdminId == null) {
            fallbackAdminId = userRepository.findAll().stream()
                    .filter(u -> u.getRole() != null && "ROLE_ADMIN".equals(u.getRole().getName()) && !u.getId().equals(targetUserId))
                    .map(User::getId)
                    .findFirst()
                    .orElse(null);
        }

        // 2. Gỡ liên kết duyệt sự kiện nếu người này từng duyệt
        entityManager.createNativeQuery(
            "UPDATE events SET approved_by = NULL WHERE approved_by = :userId"
        ).setParameter("userId", targetUserId).executeUpdate();

        // 3. Xử lý người tạo chi phí (created_by) và người check-in (check_in_by)
        if (fallbackAdminId != null) {
            entityManager.createNativeQuery(
                "UPDATE expenses SET created_by = :adminId WHERE created_by = :userId"
            ).setParameter("adminId", fallbackAdminId).setParameter("userId", targetUserId).executeUpdate();

            entityManager.createNativeQuery(
                "UPDATE check_ins SET check_in_by = :adminId WHERE check_in_by = :userId"
            ).setParameter("adminId", fallbackAdminId).setParameter("userId", targetUserId).executeUpdate();
        } else {
            entityManager.createNativeQuery(
                "DELETE FROM expenses WHERE created_by = :userId"
            ).setParameter("userId", targetUserId).executeUpdate();

            entityManager.createNativeQuery(
                "DELETE FROM check_ins WHERE check_in_by = :userId"
            ).setParameter("userId", targetUserId).executeUpdate();
        }

        // 4. Xóa các phản hồi/đánh giá (feedback) do người dùng này tạo
        entityManager.createNativeQuery(
            "DELETE FROM feedbacks WHERE user_id = :userId"
        ).setParameter("userId", targetUserId).executeUpdate();

        // 5. Xóa đăng ký / vé tham dự của người này (nếu có)
        // Keep the event locked for edits even when account deletion removes legacy ticket rows.
        entityManager.createNativeQuery(
            "UPDATE events SET has_registration_history = true WHERE id IN (SELECT event_id FROM registrations WHERE user_id = :userId)"
        ).setParameter("userId", targetUserId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM check_ins WHERE registration_id IN (SELECT id FROM registrations WHERE user_id = :userId)"
        ).setParameter("userId", targetUserId).executeUpdate();

        entityManager.createNativeQuery(
            "UPDATE seats SET is_booked = false WHERE id IN (SELECT seat_id FROM registrations WHERE user_id = :userId AND seat_id IS NOT NULL)"
        ).setParameter("userId", targetUserId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM registrations WHERE user_id = :userId"
        ).setParameter("userId", targetUserId).executeUpdate();

        // 6. Xóa các sự kiện do người này tổ chức (kèm toàn bộ dữ liệu phụ thuộc như check-in, vé, ghế, phản hồi, chi phí)
        entityManager.flush();
        entityManager.clear();
        List<Event> organizedEvents = eventRepository.findByOrganizerIdOrderByCreatedAtDesc(targetUserId);
        for (Event event : organizedEvents) {
            eventService.deleteEvent(event.getId());
        }

        // 7. Xóa tài khoản người dùng
        entityManager.createNativeQuery(
            "DELETE FROM users WHERE id = :userId"
        ).setParameter("userId", targetUserId).executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    public UserProfileDto toDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().getName())
                .isActive(user.getIsActive())
                .build();
    }
}
