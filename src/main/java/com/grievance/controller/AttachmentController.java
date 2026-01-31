package com.grievance.controller;

import com.grievance.dto.*;
import com.grievance.service.AttachmentService;
import com.grievance.entity.Attachment;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
@Tag(name = "File Attachments", description = "APIs for file uploads")
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/presigned-url")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<PresignedUploadResponse> presignSingle(@RequestBody PresignedUploadRequest req) {
        PresignedUploadResponse resp = attachmentService.generatePresignedUpload(req);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/presigned-urls")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<BulkPresignResponse> presignBulk(@RequestBody BulkPresignRequest req) {
        BulkPresignResponse resp = attachmentService.generateBulkPresigned(req);
        return ResponseEntity.ok(resp);
    }

    // confirm upload by uploadId
    @PostMapping("/confirm")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmUpload(@RequestParam String uploadId) {
        boolean ok = attachmentService.confirmUpload(uploadId);
        Map<String, Object> m = new HashMap<>();
        m.put("uploadId", uploadId);
        m.put("uploaded", ok);
        return ResponseEntity.ok(m);
    }

    // fallback server-side upload (multipart) - admin
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<AttachmentUploadResponseDTO> uploadFallback(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("grievanceId") String grievanceId,
            @RequestParam(value = "fileType", required = false, defaultValue = "OTHER") String fileType
    ) throws IOException {
        AttachmentUploadResponseDTO saved = attachmentService.uploadFilesServerSide(files, grievanceId, fileType);
        return ResponseEntity.ok(saved);
    }
}
