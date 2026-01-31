package com.grievance.repository;

import com.grievance.entity.Attachment;
import com.grievance.entity.Grievance;
import com.grievance.entity.AttachmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    Optional<Attachment> findByGrievance_GrievanceIdAndUploadId(Long grievanceId, String uploadId);

    boolean existsByGrievanceAndUploadId(Grievance grievance, String uploadId);

    Optional<Attachment> findByUploadId(String uploadId);

    List<Attachment> findByGrievance_GrievanceId(Long grievanceId);

    List<Attachment> findByGrievance_GrievanceIdAndStatus(Long grievanceId, AttachmentStatus status);

    void deleteByGrievance_GrievanceId(Long grievanceId);
}
