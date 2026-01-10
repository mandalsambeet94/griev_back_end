package com.grievance.controller;

import com.grievance.dto.GrievanceDTO;
import com.grievance.dto.GrievanceRequest;
import com.grievance.service.GrievanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grievances")
@RequiredArgsConstructor
@Tag(name = "Grievance Management", description = "APIs for managing grievances")
@SecurityRequirement(name = "bearerAuth")
public class GrievanceController {

    private final GrievanceService grievanceService;

    @PostMapping
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Save grievance form")
    public ResponseEntity<GrievanceDTO> saveGrievance(
            @Valid @RequestBody GrievanceRequest request) {
        GrievanceDTO grievance = grievanceService.saveGrievance(request);
        return ResponseEntity.ok(grievance);
    }

    @PutMapping("/{grievanceId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update grievance details (Admin only)")
    public ResponseEntity<GrievanceDTO> updateGrievance(
            @PathVariable Long grievanceId,
            @Valid @RequestBody GrievanceRequest request) {
        GrievanceDTO grievance = grievanceService.updateGrievance(grievanceId, request);
        return ResponseEntity.ok(grievance);
    }

    @GetMapping("/{grievanceId}")
    @Operation(summary = "Get grievance by ID")
    public ResponseEntity<GrievanceDTO> getGrievance(@PathVariable Long grievanceId) {
        GrievanceDTO grievance = grievanceService.getGrievanceById(grievanceId);
        return ResponseEntity.ok(grievance);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all grievances (paginated)")
    public ResponseEntity<Page<GrievanceDTO>> getAllGrievances(
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        Page<GrievanceDTO> grievances = grievanceService.getAllGrievances(pageable);
        return ResponseEntity.ok(grievances);
    }

    @GetMapping
    @Operation(summary = "Get grievances by filters")
    public ResponseEntity<List<GrievanceDTO>> getGrievancesByFilters(
            @RequestParam(required = false) String block,
            @RequestParam(required = false) String gp,
            @RequestParam(required = false) String villageSahi,
            @RequestParam(required = false) String name) {
        List<GrievanceDTO> grievances = grievanceService.getGrievancesByFilters(
                block, gp, villageSahi, name);
        return ResponseEntity.ok(grievances);
    }

    @DeleteMapping("/{grievanceId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete grievance (Admin only)")
    public ResponseEntity<Void> deleteGrievance(@PathVariable Long grievanceId) {
        grievanceService.deleteGrievance(grievanceId);
        return ResponseEntity.noContent().build();
    }
}