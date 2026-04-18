package com.policyserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StageCountDto {
    private String stage;       // EXTRACT | APPROVE | TECH_BUCKET | SANCTION | PAID
    private String label;
    private Long   count;
    private Long   previousCount;
    private Integer agingCount;
    private Integer agingThresholdDays;
    private String color;
    private String icon;
}
