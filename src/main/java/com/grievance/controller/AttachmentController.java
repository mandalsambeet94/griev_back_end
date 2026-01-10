package com.grievance.controller;

import com.grievance.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
@Tag(name = "File Attachments", description = "APIs for file uploads")
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {

    private final S3Service s3Service;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Upload file to S3")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "uploads") String folder) {

        try {
            String fileUrl = s3Service.uploadFile(file, folder);

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("fileName", file.getOriginalFilename());
            response.put("contentType", file.getContentType());
            response.put("size", String.valueOf(file.getSize()));

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @GetMapping("/presigned-url")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Generate pre-signed URL for direct upload")
    public ResponseEntity<Map<String, String>> generatePresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {

        String presignedUrl = s3Service.generatePresignedUrl(fileName, contentType);

        Map<String, String> response = new HashMap<>();
        response.put("presignedUrl", presignedUrl);
        response.put("fileName", fileName);

        return ResponseEntity.ok(response);
    }
}