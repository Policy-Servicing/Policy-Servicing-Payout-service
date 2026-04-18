package com.policyserve.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {

    private String policyNo;
    private String requestId;
    private String actionTaken;    // APPROVE | SANCTION
    private String performedBy;
    private String remarks;
    private String status;         // SUCCESS | ERROR

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date performedAt;
}
