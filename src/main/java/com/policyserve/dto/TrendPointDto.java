package com.policyserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TrendPointDto {
    private String date;
    private Long   extract;
    private Long   approve;
    private Long   techBucket;
    private Long   sanction;
    private Long   paid;
}
