package com.policyserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PolicyRecordDto {
    private String     policyNo;
    private String     productId;
    private String     moduleCode;
    private String     holderName;
    private String     stage;
    private BigDecimal amount;
    private LocalDateTime enteredAt;
    private Integer    agingDays;
    private String     status;
    private String     remarks;
}
