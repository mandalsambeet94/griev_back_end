package com.grievance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AttachmentUploadResponseDTO {

    private String message;
    private Long grievanceId;
    private Integer totalUploaded;
    private List<AttachmentUploadItemDTO> attachments;
}

