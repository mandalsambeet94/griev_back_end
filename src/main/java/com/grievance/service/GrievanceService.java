package com.grievance.service;

import com.grievance.dto.AttachmentDTO;
import com.grievance.dto.GrievanceDTO;
import com.grievance.dto.GrievanceFilter;
import com.grievance.dto.GrievanceRequest;
import com.grievance.entity.Attachment;
import com.grievance.entity.Grievance;
import com.grievance.entity.User;
import com.grievance.exception.ResourceNotFoundException;
import com.grievance.exception.UnauthorizedException;
import com.grievance.repository.AttachmentRepository;
import com.grievance.repository.GrievanceRepository;
import com.grievance.repository.GrievanceSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final AttachmentRepository attachmentRepository;
    private final AuthService authService;
    private final S3Service s3Service;

    @Transactional
    public GrievanceDTO saveGrievance(GrievanceRequest request) {
        User currentUser = authService.getCurrentUser();

        Grievance grievance = new Grievance();
        mapRequestToEntity(request, grievance);

        // Set collected by current user
        grievance.setCollectedBy(currentUser);
        //grievance.setAgentName(currentUser.getName());

        // Set default status
        if (grievance.getStatus() == null) {
            grievance.setStatus(Grievance.GrievanceStatus.PENDING);
        }

        // Save grievance
        Grievance savedGrievance = grievanceRepository.save(grievance);

        // Save attachments if provided
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            saveAttachments(savedGrievance, request.getAttachments());
        }

        return GrievanceDTO.fromEntity(savedGrievance);
    }

    @Transactional
    public GrievanceDTO updateGrievance(Long grievanceId, GrievanceRequest request) {
        User currentUser = authService.getCurrentUser();

        // Check if user is admin
        if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new UnauthorizedException("Only admin can update grievances");
        }

        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        mapRequestToEntity(request, grievance);

        // Update timestamp
        grievance.setUpdatedAt(LocalDateTime.now());

        Grievance updatedGrievance = grievanceRepository.save(grievance);

        // Update attachments if provided
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            // Delete existing attachments
            attachmentRepository.deleteByGrievance_GrievanceId(grievanceId);
            // Save new attachments
            saveAttachments(updatedGrievance, request.getAttachments());
        }

        return GrievanceDTO.fromEntity(updatedGrievance);
    }

    public GrievanceDTO getGrievanceById(Long grievanceId) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        // Load attachments
        List<Attachment> attachments = attachmentRepository.findByGrievance_GrievanceId(grievanceId);
        grievance.setAttachments(attachments);

        return GrievanceDTO.fromEntity(grievance);
    }

    public Page<GrievanceDTO> getAllGrievances(Pageable pageable) {
        User currentUser = authService.getCurrentUser();

        Page<Grievance> grievances;

        // If user is agent, only show their grievances
        if (currentUser.getRole().equals(User.Role.AGENT)) {
            grievances = grievanceRepository.findByCollectedBy(currentUser, pageable);
        } else {
            // Admin can see all
            grievances = grievanceRepository.findAll(pageable);
        }

        // Load attachments for each grievance
        grievances.forEach(grievance -> {
            List<Attachment> attachments = attachmentRepository.findByGrievance_GrievanceId(
                    grievance.getGrievanceId());
            grievance.setAttachments(attachments);
        });

        return grievances.map(GrievanceDTO::fromEntity);
    }

    /*public List<GrievanceDTO> getGrievancesByFilters(String block, String gp,
                                                     String villageSahi, String name) {
        List<Grievance> grievances = grievanceRepository.findByFilters(block, gp, villageSahi, name);

        // Load attachments for each grievance
        grievances.forEach(grievance -> {
            List<Attachment> attachments = attachmentRepository.findByGrievance_GrievanceId(
                    grievance.getGrievanceId());
            grievance.setAttachments(attachments);
        });

        return grievances.stream()
                .map(GrievanceDTO::fromEntity)
                .collect(Collectors.toList());
    }*/

    public List<GrievanceDTO> getGrievancesByFilters(
            String block,
            String gp,
            String villageSahi,
            String wardNo,
            LocalDate startDate,
            LocalDate endDate,
            Grievance.GrievanceStatus status,
            String name, String contact, String fatherSpouseName) {

        List<Grievance> grievances ;
        LocalDateTime startOfDay = LocalDateTime.of(1970, 1, 1, 0, 0);;
        LocalDateTime endOfDay =LocalDateTime.of(3000, 1, 1, 0, 0);;


        if (null !=  startDate && null != endDate) {
            startOfDay = startDate.atStartOfDay();
            endOfDay = endDate.atStartOfDay();
        }

        if (status == null) {
            grievances = grievanceRepository.findWithoutStatus(
                    block, gp, villageSahi, wardNo,startOfDay,endOfDay,contact,fatherSpouseName, name
            );
        } else {
            grievances = grievanceRepository.findWithStatus(
                    block, gp, villageSahi, wardNo, startOfDay,endOfDay,contact,fatherSpouseName, status, name
            );
        }

        // Load attachments for each grievance
        grievances.forEach(grievance -> {
            List<Attachment> attachments =
                    attachmentRepository.findByGrievance_GrievanceId(
                            grievance.getGrievanceId());
            grievance.setAttachments(attachments);
        });

        return grievances.stream()
                .map(GrievanceDTO::fromEntity)
                .collect(Collectors.toList());
    }


    public List<GrievanceDTO> getGrievances(GrievanceFilter filter) {
        List<Grievance> grievances =
                grievanceRepository.findAll(
                        GrievanceSpecification.filter(filter)
                );

        return grievances.stream()
                .map(GrievanceDTO::fromEntity)
                .toList();
    }



    @Transactional
    public void deleteGrievance(Long grievanceId) {
        User currentUser = authService.getCurrentUser();

        // Check if user is admin
        if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new UnauthorizedException("Only admin can delete grievances");
        }

        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        // Delete attachments from S3
        if (grievance.getAttachments() != null) {
            for (Attachment attachment : grievance.getAttachments()) {
                s3Service.deleteFile(attachment.getS3Key());
            }
        }

        // Delete from database
        attachmentRepository.deleteByGrievance_GrievanceId(grievanceId);
        grievanceRepository.delete(grievance);
    }

    private void mapRequestToEntity(GrievanceRequest request, Grievance grievance) {
        grievance.setBlock(request.getBlock());
        grievance.setGp(request.getGp());
        grievance.setVillageSahi(request.getVillageSahi());
        grievance.setAddress(request.getAddress());
        grievance.setWardNo(request.getWardNo());
        grievance.setName(request.getName());
        grievance.setFatherSpouseName(request.getFatherSpouseName());
        grievance.setContact(request.getContact());
        grievance.setTopic1(request.getTopic1());
        grievance.setTopic2(request.getTopic2());
        grievance.setTopic3(request.getTopic3());
        grievance.setTopic4(request.getTopic4());
        grievance.setTopic5(request.getTopic5());
        grievance.setGrievanceDetails(request.getGrievanceDetails());
        grievance.setAgentRemarks(request.getAgentRemarks());
        grievance.setAgentName(request.getAgentName());
        grievance.setWorkGivenTo(request.getWorkGivenTo());

        if (request.getStatus() != null) {
            grievance.setStatus(Grievance.GrievanceStatus.valueOf(request.getStatus().toUpperCase()));
        }

        grievance.setAdminDate(request.getAdminDate());
        grievance.setAdminRemarks(request.getAdminRemarks());
    }

    private void saveAttachments(Grievance grievance, List<AttachmentDTO> attachmentDTOs) {
        for (AttachmentDTO dto : attachmentDTOs) {
            Attachment attachment = new Attachment();
            attachment.setGrievance(grievance);
            attachment.setS3Url(dto.getS3Url());
            attachment.setS3Key(dto.getS3Key());
            attachment.setFileType(dto.getFileType());
            attachment.setFileName(dto.getFileName());
            attachment.setFileSize(dto.getFileSize());

            attachmentRepository.save(attachment);
        }
    }
}