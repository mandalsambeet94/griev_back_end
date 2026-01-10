package com.grievance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RdsLifecycleService {

    private final RdsClient rdsClient;

    @Value("${aws.rds.instance-id:grievance-db}")
    private String rdsInstanceId;

    public String getRdsStatus() {
        try {
            DescribeDbInstancesRequest request = DescribeDbInstancesRequest.builder()
                    .dbInstanceIdentifier(rdsInstanceId)
                    .build();

            DescribeDbInstancesResponse response = rdsClient.describeDBInstances(request);

            if (!response.dbInstances().isEmpty()) {
                return response.dbInstances().get(0).dbInstanceStatus();
            }
            return "NOT_FOUND";
        } catch (Exception e) {
            log.error("Error getting RDS status: {}", e.getMessage());
            return "ERROR";
        }
    }

    public void startRds() {
        try {
            StartDbInstanceRequest request = StartDbInstanceRequest.builder()
                    .dbInstanceIdentifier(rdsInstanceId)
                    .build();

            rdsClient.startDBInstance(request);
            log.info("RDS instance {} started at {}", rdsInstanceId, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error starting RDS: {}", e.getMessage());
        }
    }

    public void stopRds() {
        try {
            StopDbInstanceRequest request = StopDbInstanceRequest.builder()
                    .dbInstanceIdentifier(rdsInstanceId)
                    .build();

            rdsClient.stopDBInstance(request);
            log.info("RDS instance {} stopped at {}", rdsInstanceId, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Error stopping RDS: {}", e.getMessage());
        }
    }

    public boolean isRdsAvailable() {
        String status = getRdsStatus();
        return "available".equalsIgnoreCase(status) ||
                "starting".equalsIgnoreCase(status);
    }

    // This method checks if RDS should be stopped based on inactivity
    // You can integrate with your activity tracking system
    public boolean shouldStopRds() {
        // Implement your logic here
        // Check last activity timestamp, time of day, etc.
        return false;
    }
}