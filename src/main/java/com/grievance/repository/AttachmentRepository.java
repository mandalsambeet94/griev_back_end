package com.grievance.repository;

import com.grievance.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByGrievance_GrievanceId(Long grievanceId);

    void deleteByGrievance_GrievanceId(Long grievanceId);
}