package com.grievance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BulkPresignResponse {
    private List<PresignedUploadResponse> uploads;

}
