package com.policyserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RunHistoryDto {
    private String        id;
    private String        schedulerId;
    private LocalDateTime runTime;
    private String        status;          // SUCCESS | FAILED | RUNNING
    private Long          recordsProcessed;
    private Long          durationMs;
    private String        errorMessage;
    private String        triggeredBy;     // SCHEDULER | MANUAL
}
