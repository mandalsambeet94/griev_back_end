package com.grievance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class GrievanceRequest {

    @NotBlank(message = "Block is required")
    private String block;

    private String gp;
    private String villageSahi;
    private String address;
    private String wardNo;

    @NotBlank(message = "Name is required")
    private String name;

    private String fatherSpouseName;
    private String contact;

    private String topic1;
    private String topic2;
    private String topic3;
    private String topic4;
    private String topic5;

    @NotBlank(message = "Grievance details are required")
    private String grievanceDetails;

    private String agentRemarks;
    private String agentName;
    private String workGivenTo;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate adminDate;

    private String adminRemarks;

    // For attachments
    private List<AttachmentDTO> attachments;
}