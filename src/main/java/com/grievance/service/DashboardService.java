package com.grievance.service;

import com.grievance.dto.DashboardResponse;
import com.grievance.dto.GrievanceDTO;
import com.grievance.entity.Attachment;
import com.grievance.entity.Grievance;
import com.grievance.entity.User;
import com.grievance.repository.GrievanceRepository;
import com.grievance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final GrievanceRepository grievanceRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public DashboardResponse getAdminDashboard(LocalDate startDate, LocalDate endDate) {
        DashboardResponse response = new DashboardResponse();

        // Set date range
        LocalDateTime startDateTime = (startDate != null) ?
                startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime endDateTime = (endDate != null) ?
                endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        // Total statistics
        response.setTotalGrievances(grievanceRepository.count());
        /*response.setActiveAgents(userRepository.findByRoleAndIsActiveTrue(
                com.grievance.entity.User.Role.AGENT));*/
        response.setTotalUsers((long) userRepository.findByRoleAndIsActiveTrue(User.Role.AGENT).size());

        // Grievance status breakdown
        Map<String, Long> statusCounts = new HashMap<>();
        grievanceRepository.countByStatus().forEach(obj -> {
            statusCounts.put(String.valueOf(obj[0]), (Long) obj[1]);
        });
        response.setGrievanceStatusCounts(statusCounts);

        // Block-wise distribution
        Map<String, Long> blockDistribution = new HashMap<>();
        grievanceRepository.countByBlock().forEach(obj -> {
            blockDistribution.put(String.valueOf(obj[0]), (Long) obj[1]);
        });
        response.setBlockDistribution(blockDistribution);

        // Daily trend (last 30 days)
        Map<String, Long> dailyTrend = new HashMap<>();
        grievanceRepository.countByDate(LocalDateTime.now().minusDays(30)).forEach(obj -> {
            dailyTrend.put(String.valueOf(obj[0]), (Long) obj[1]);
        });
        response.setDailyTrend(dailyTrend);

        // Top performing agents
        // Recent Activity
        List<Grievance> grievances = grievanceRepository.findTop10ByOrderByUpdatedAtDesc();

        response.setRecentActivity(grievances.stream()
                .map(GrievanceDTO::fromEntity)
                .collect(Collectors.toList()));

        return response;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getAgentDashboard(LocalDate startDate, LocalDate endDate) {
        DashboardResponse response = new DashboardResponse();
        User currentUser = authService.getCurrentUser();

        // Set date range
        LocalDateTime startDateTime = (startDate != null) ?
                startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime endDateTime = (endDate != null) ?
                endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        // Agent-specific statistics
        Long totalGrievances = grievanceRepository.countByAgent(currentUser);
        response.setTotalGrievances(totalGrievances != null ? totalGrievances : 0L);

        // Grievance status breakdown for this agent
        Map<String, Long> statusCounts = new HashMap<>();
        for (com.grievance.entity.Grievance.GrievanceStatus status :
                com.grievance.entity.Grievance.GrievanceStatus.values()) {
            Long count = grievanceRepository.countByStatusAndAgent(status, currentUser);
            if (count != null && count > 0) {
                statusCounts.put(status.name(), count);
            }
        }
        response.setGrievanceStatusCounts(statusCounts);

        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZoneId utcZone = ZoneId.of("UTC");

        LocalDate todayIst = LocalDate.now(istZone);

// IST → ZonedDateTime
        ZonedDateTime startOfDayIst = todayIst.atStartOfDay(istZone);
        ZonedDateTime endOfDayIst = todayIst.plusDays(1).atStartOfDay(istZone);

// Convert to UTC
        LocalDateTime startUtc = startOfDayIst.withZoneSameInstant(utcZone).toLocalDateTime();
        LocalDateTime endUtc = endOfDayIst.withZoneSameInstant(utcZone).toLocalDateTime();

        List<Grievance> grievances =
                grievanceRepository.countTodayByAgent(startUtc, endUtc, currentUser);

        response.setRecentActivity(grievances.stream()
                .map(GrievanceDTO::fromEntity)
                .collect(Collectors.toList()));

        return response;
    }
}