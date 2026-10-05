package com.qlsk.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng chọn tập tin ảnh"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Chỉ hỗ trợ tải lên tập tin hình ảnh (JPG, PNG, GIF, WEBP)"));
        }

        try {
            String originalFileName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "image.png"));
            String extension = "";
            int extIndex = originalFileName.lastIndexOf(".");
            if (extIndex > 0) {
                extension = originalFileName.substring(extIndex).toLowerCase();
            } else {
                extension = ".png";
            }

            String newFileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = this.uploadDir.resolve(newFileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            String fileUrl = "/uploads/" + newFileName;
            return ResponseEntity.ok(Map.of("url", fileUrl));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Không thể lưu tập tin: " + ex.getMessage()));
        }
    }
}
