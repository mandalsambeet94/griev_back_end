package com.grievance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponse {

    private Long totalGrievances;
    private Long pendingGrievances;
    private Long completedGrievances;
    private Long activeAgents;
    private Long totalUsers;

    // Status breakdown
    private Map<String, Long> grievanceStatusCounts;

    // Block-wise distribution
    private Map<String, Long> blockDistribution;

    // Daily trend
    private Map<String, Long> dailyTrend;

    // Agent performance
    private Map<String, Long> agentPerformance;

    // Recent activity
    private List<GrievanceDTO> recentActivity;
}