package com.grievance.controller;

import com.grievance.dto.DashboardResponse;
import com.grievance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "APIs for dashboard statistics")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Admin dashboard statistics")
    public ResponseEntity<DashboardResponse> getAdminDashboard(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        DashboardResponse dashboard = dashboardService.getAdminDashboard(startDate, endDate);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/agent")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Agent dashboard statistics")
    public ResponseEntity<DashboardResponse> getAgentDashboard(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        DashboardResponse dashboard = dashboardService.getAgentDashboard(startDate, endDate);
        return ResponseEntity.ok(dashboard);
    }
}