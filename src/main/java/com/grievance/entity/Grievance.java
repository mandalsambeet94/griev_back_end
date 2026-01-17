package com.grievance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grievances_excel")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grievance_id")
    private Long grievanceId;

    @Column(length = 100)
    private String block;

    @Column(length = 100)
    private String gp;

    @Column(name = "village_sahi", length = 150)
    private String villageSahi;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "ward_no", length = 20)
    private String wardNo;

    @Column(name= "name", length = 150)
    private String name;

    @Column(name = "father_spouse_name", length = 150)
    private String fatherSpouseName;

    @Column(length = 20)
    private String contact;

    @Column(name = "topic_1", length = 500)
    private String topic1;

    @Column(name = "topic_2", length = 500)
    private String topic2;

    @Column(name = "topic_3", length = 500)
    private String topic3;

    @Column(name = "topic_4", length = 500)
    private String topic4;

    @Column(name = "topic_5", length = 500)
    private String topic5;

    @Column(name = "grievance_details", columnDefinition = "TEXT")
    private String grievanceDetails;

    @Column(name = "agent_remarks", length = 150)
    private String agentRemarks;

    @Column(name = "agent_name", length = 150)
    private String agentName;

    @Column(name = "work_given_to", length = 150)
    private String workGivenTo;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private GrievanceStatus status = GrievanceStatus.PENDING;

    @Column(name = "admin_date")
    private LocalDate adminDate;

    @Column(name = "admin_remarks", columnDefinition = "TEXT")
    private String adminRemarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "grievance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attachment> attachments = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by")
    private User collectedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    public enum GrievanceStatus {
        PENDING, IN_PROGRESS, COMPLETED, REJECTED, REOPENED
    }
}