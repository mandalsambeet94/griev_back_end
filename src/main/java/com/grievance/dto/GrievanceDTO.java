package com.grievance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.grievance.entity.Grievance;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrievanceDTO {

    private Long grievanceId;
    private String block;
    private String gp;
    private String villageSahi;
    private String address;
    private String wardNo;
    private String name;
    private String fatherSpouseName;
    private String contact;
    private String topic1;
    private String topic2;
    private String topic3;
    private String topic4;
    private String topic5;
    private String grievanceDetails;
    private String agentRemarks;
    private String agentName;
    private String workGivenTo;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate adminDate;

    private String adminRemarks;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private UserDTO collectedBy;
    private UserDTO assignedTo;

    private List<AttachmentDTO> attachments;

    public static GrievanceDTO fromEntity(Grievance grievance) {
        GrievanceDTO dto = new GrievanceDTO();
        dto.setGrievanceId(grievance.getGrievanceId());
        dto.setBlock(grievance.getBlock());
        dto.setGp(grievance.getGp());
        dto.setVillageSahi(grievance.getVillageSahi());
        dto.setAddress(grievance.getAddress());
        dto.setWardNo(grievance.getWardNo());
        dto.setName(grievance.getName());
        dto.setFatherSpouseName(grievance.getFatherSpouseName());
        dto.setContact(grievance.getContact());
        dto.setTopic1(grievance.getTopic1());
        dto.setTopic2(grievance.getTopic2());
        dto.setTopic3(grievance.getTopic3());
        dto.setTopic4(grievance.getTopic4());
        dto.setTopic5(grievance.getTopic5());
        dto.setGrievanceDetails(grievance.getGrievanceDetails());
        dto.setAgentRemarks(grievance.getAgentRemarks());
        dto.setAgentName(grievance.getAgentName());
        dto.setWorkGivenTo(grievance.getWorkGivenTo());
        dto.setStatus(grievance.getStatus() != null ? grievance.getStatus().name() : null);
        dto.setAdminDate(grievance.getAdminDate());
        dto.setAdminRemarks(grievance.getAdminRemarks());
        dto.setCreatedAt(grievance.getCreatedAt());
        dto.setUpdatedAt(grievance.getUpdatedAt());

        if (grievance.getCollectedBy() != null) {
            dto.setCollectedBy(UserDTO.fromEntity(grievance.getCollectedBy()));
        }

        if (grievance.getAssignedTo() != null) {
            dto.setAssignedTo(UserDTO.fromEntity(grievance.getAssignedTo()));
        }

        if (grievance.getAttachments() != null) {
            dto.setAttachments(grievance.getAttachments().stream()
                    .map(AttachmentDTO::fromEntity)
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}