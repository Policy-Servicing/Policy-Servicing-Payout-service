package com.policyserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DrillDownResponseDto {
    private String           stage;
    private List<PolicyRecordDto> records;
    private Long             totalCount;
    private Integer          page;
    private Integer          size;
}
