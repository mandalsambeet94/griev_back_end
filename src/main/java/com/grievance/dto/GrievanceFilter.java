package com.grievance.dto;

import com.grievance.entity.Grievance;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class GrievanceFilter {

    private String block;
    private String gp;
    private String villageSahi;
    private String wardNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private Grievance.GrievanceStatus status;
    private String name;
    private String contact;
    private String fatherSpouseName;
    private String topic;
}

