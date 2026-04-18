package com.policyserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SchedulerDto {
    private String        id;
    private String        name;
    private String        jobClass;
    private String        moduleCode;
    private String        cronExpression;
    private String        status;
    private Boolean       enabled;
    private LocalDateTime lastRunTime;
    private String        lastRunStatus;
    private LocalDateTime nextRunTime;
    private Long          recordsProcessed;
    private Long          durationMs;
    private String        errorMessage;
    private String        description;
}
