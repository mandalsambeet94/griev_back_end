package com.grievance.service;

import com.grievance.dto.*;
import com.grievance.entity.Attachment;
import com.grievance.entity.AttachmentStatus;
import com.grievance.entity.Grievance;
import com.grievance.exception.ResourceNotFoundException;
import com.grievance.repository.AttachmentRepository;
import com.grievance.repository.GrievanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final GrievanceRepository grievanceRepository;
    private final S3Service s3Service;

    private static final int PRESIGN_EXPIRY_SECONDS = 15 * 60;

    // generate single presigned upload (idempotent)
    @Transactional
    public PresignedUploadResponse generatePresignedUpload(PresignedUploadRequest req) {
        //validate content type
        s3Service.validateContentType(req.getFileName(), req.getContentType());
        // validate grievance exists
        Grievance grievance = grievanceRepository.findById(Long.valueOf(req.getGrievanceId()))
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        String uploadId = req.getUploadId() == null ? UUID.randomUUID().toString() : req.getUploadId();

        // deterministic key
        String key = buildKey(String.valueOf(grievance.getGrievanceId()), uploadId, req.getFileName());

        String s3Url = s3Service.generateFileUrl(key);

        // check existing DB row by grievance+uploadId
        Attachment saved = attachmentRepository.findByGrievance_GrievanceIdAndUploadId(grievance.getGrievanceId(), uploadId)
                .orElseGet(() -> {
                    Attachment a = new Attachment();
                    a.setGrievance(grievance);
                    a.setUploadId(uploadId);
                    a.setS3Key(key);
                    a.setS3Url(s3Url);
                    a.setFileName(req.getFileName());
                    a.setFileType(req.getFileType());
                    a.setStatus(AttachmentStatus.PENDING);
                    return attachmentRepository.save(a);
                });

        String presignedUrl = s3Service.generatePresignedPutUrl(key, req.getContentType(), PRESIGN_EXPIRY_SECONDS);

        return new PresignedUploadResponse(uploadId, key, presignedUrl, saved.getFileName(), saved.getFileType());
    }

    // bulk presign
    @Transactional
    public BulkPresignResponse generateBulkPresigned(BulkPresignRequest req) {
        Grievance grievance = grievanceRepository.findById(Long.valueOf(req.getGrievanceId()))
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        List<PresignedUploadResponse> responses = new ArrayList<>();

        for (PresignedUploadRequest fileReq : req.getFiles()) {
            fileReq.setGrievanceId(req.getGrievanceId());
            PresignedUploadResponse resp = generatePresignedUpload(fileReq);
            responses.add(resp);
        }

        return new BulkPresignResponse(responses);
    }

    // confirm upload: HEAD S3 & mark uploaded
    @Transactional
    public boolean confirmUpload(String uploadId) {
        Attachment attachment = attachmentRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment (uploadId) not found"));

        // HEAD S3
        HeadObjectResponse head = s3Service.headObject(attachment.getS3Key());
        if (head == null) {
            // not uploaded yet
            return false;
        }

        // set s3Url, size, status
        attachment.setS3Url(s3Service.generateFileUrl(attachment.getS3Key()));
        attachment.setFileSize(head.contentLength());
        attachment.setStatus(AttachmentStatus.UPLOADED);
        attachmentRepository.save(attachment);
        return true;
    }

    // fallback server-side upload for MultipartFile[] - admin usage
    @Transactional
    public AttachmentUploadResponseDTO uploadFilesServerSide(MultipartFile[] files, String grievanceId, String fileType) throws IOException {
        Grievance grievance = grievanceRepository.findById(Long.valueOf(grievanceId))
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        List<AttachmentUploadItemDTO> responseList = new ArrayList<>();
        for (MultipartFile f : files) {
            String uploadId = UUID.randomUUID().toString();
            String key = buildKey(String.valueOf(grievance.getGrievanceId()), uploadId, f.getOriginalFilename());
            String s3Url = s3Service.uploadFile(f, key);

            Attachment a = new Attachment();
            a.setGrievance(grievance);
            a.setUploadId(uploadId);
            a.setS3Key(key);
            a.setS3Url(s3Url);
            a.setFileName(f.getOriginalFilename());
            a.setFileType(fileType);
            a.setFileSize(f.getSize());
            a.setStatus(AttachmentStatus.UPLOADED);

            Attachment saved = attachmentRepository.save(a);

            responseList.add(
                    new AttachmentUploadItemDTO(
                            saved.getAttachmentId(),
                            saved.getUploadId(),
                            saved.getFileName(),
                            saved.getFileType(),
                            saved.getFileSize(),
                            saved.getS3Url(),
                            saved.getStatus().name()
                    )
            );
        }
        return new AttachmentUploadResponseDTO(
                "Files uploaded successfully",
                grievance.getGrievanceId(),
                responseList.size(),
                responseList
        );
    }

    private String buildKey(String grievanceId, String uploadId, String originalFileName) {
        String sanitized = originalFileName == null ? "file" : originalFileName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        return String.format("grievances/%s/%s_%s", grievanceId, uploadId, sanitized);
    }
}
