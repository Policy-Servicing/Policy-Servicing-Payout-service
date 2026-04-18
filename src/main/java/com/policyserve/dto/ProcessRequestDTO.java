package com.policyserve.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequestDTO {

    @NotBlank(message = "Policy number is required")
    private String policyNo;

    @NotBlank(message = "Request ID is required")
    private String requestId;

    @NotBlank(message = "Action is required")
    private String action;   // APPROVE | SANCTION

    private String remarks;
}
