package com.qlsk.controller;

import com.qlsk.dto.AuthDtos.UserProfileDto;
import com.qlsk.security.UserPrincipal;
import com.qlsk.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/profile")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getUserProfile(principal.getId()));
    }

    @PutMapping("/users/profile")
    public ResponseEntity<UserProfileDto> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                        @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.updateProfile(
                principal.getId(),
                body.get("fullName"),
                body.get("phone"),
                body.get("oldPassword"),
                body.get("newPassword")));
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PatchMapping("/admin/users/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> toggleUserStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.toggleUserStatus(principal.getId(), id));
    }

    @PatchMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> changeUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.changeUserRole(id, body.get("role")));
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> createUser(@RequestBody com.qlsk.dto.AuthDtos.AdminUserFormRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @PutMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> updateUser(
            @PathVariable Long id,
            @RequestBody com.qlsk.dto.AuthDtos.AdminUserFormRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateUser(principal.getId(), id, request));
    }

    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteUser(principal.getId(), id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa tài khoản thành công"));
    }
}
