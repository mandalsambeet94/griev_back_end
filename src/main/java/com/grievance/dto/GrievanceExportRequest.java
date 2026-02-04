package com.grievance.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GrievanceExportRequest {
    @NotEmpty
    private List<Long> grievanceIds;
    @NotNull
    private ExportFormat format; // excel or pdf
}

