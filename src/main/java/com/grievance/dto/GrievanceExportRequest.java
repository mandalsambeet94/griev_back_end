package com.grievance.dto;

import lombok.Data;

import java.util.List;

@Data
public class GrievanceExportRequest {
    private List<Long> grievanceIds;
    private String format; // excel or pdf
}

