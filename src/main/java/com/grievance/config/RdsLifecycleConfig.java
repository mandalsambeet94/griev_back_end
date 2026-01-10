package com.grievance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class RdsLifecycleConfig {

    @Value("${aws.rds.instance-id:grievance-db}")
    private String rdsInstanceId;

    @Value("${aws.region:ap-south-1}")
    private String region;

    // This method will check RDS status every hour
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void checkAndManageRds() {
        // Implementation in RdsLifecycleService
    }

    // Start RDS at 6 AM IST daily if needed
    @Scheduled(cron = "0 30 6 * * *")
    public void startRdsIfNeeded() {
        // Implementation in RdsLifecycleService
    }

    // Stop RDS at 10 PM IST daily if idle
    @Scheduled(cron = "0 30 22 * * *")
    public void stopRdsIfIdle() {
        // Implementation in RdsLifecycleService
    }
}