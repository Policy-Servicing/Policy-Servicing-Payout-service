package com.policyserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StageSummaryDto {
    private String module;
    private String productId;
    private String dateRange;
    private List<StageCountDto> stageCounts;
    private Long totalPending;
    private Long totalAmount;
    private Integer slaBreaches;
    private LocalDateTime lastUpdated;
}
