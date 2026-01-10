package com.grievance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.grievance.entity.Attachment;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentDTO {

    private Long attachmentId;
    private Long grievanceId;
    private String s3Url;
    private String s3Key;
    private String fileType;
    private String fileName;
    private Long fileSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadedAt;

    public static AttachmentDTO fromEntity(Attachment attachment) {
        if (attachment == null) {
            return null;
        }

        AttachmentDTO dto = new AttachmentDTO();
        dto.setAttachmentId(attachment.getAttachmentId());
        dto.setGrievanceId(attachment.getGrievance() != null ?
                attachment.getGrievance().getGrievanceId() : null);
        dto.setS3Url(attachment.getS3Url());
        dto.setS3Key(attachment.getS3Key());
        dto.setFileType(attachment.getFileType());
        dto.setFileName(attachment.getFileName());
        dto.setFileSize(attachment.getFileSize());
        dto.setUploadedAt(attachment.getUploadedAt());

        return dto;
    }
}