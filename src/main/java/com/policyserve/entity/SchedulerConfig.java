package com.policyserve.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "SCHEDULER_CONFIG")
public class SchedulerConfig {

    @Id
    @Column(name = "SCHEDULER_ID", length = 20)
    private String schedulerId;

    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "JOB_CLASS", length = 200, nullable = false)
    private String jobClass;

    @Column(name = "MODULE_CODE", length = 10)
    private String moduleCode;

    @Column(name = "CRON_EXPRESSION", length = 50, nullable = false)
    private String cronExpression;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "ENABLED")
    private Boolean enabled;

    @Column(name = "LAST_RUN_TIME")
    private LocalDateTime lastRunTime;

    @Column(name = "LAST_RUN_STATUS", length = 20)
    private String lastRunStatus;

    @Column(name = "NEXT_RUN_TIME")
    private LocalDateTime nextRunTime;

    @Column(name = "RECORDS_PROCESSED")
    private Long recordsProcessed;

    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "ERROR_MESSAGE", length = 500)
    private String errorMessage;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;
}
