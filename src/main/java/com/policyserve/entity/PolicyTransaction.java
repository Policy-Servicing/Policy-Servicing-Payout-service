package com.policyserve.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "POLICY_STAGE")
public class PolicyTransaction {

    @Id
    @Column(name = "POLICY_NO", length = 20)
    private String policyNo;

    @Column(name = "PRODUCT_ID", length = 10, nullable = false)
    private String productId;

    @Column(name = "MODULE_CODE", length = 10, nullable = false)
    private String moduleCode;

    @Column(name = "HOLDER_NAME", length = 100)
    private String holderName;

    @Column(name = "CURRENT_STAGE", length = 20, nullable = false)
    private String currentStage;   // EXTRACT | APPROVE | TECH_BUCKET | SANCTION | PAID

    @Column(name = "AMOUNT", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "ENTERED_AT")
    private LocalDateTime enteredAt;

    @Column(name = "STATUS", length = 20)
    private String status;         // PENDING | PROCESSING | FAILED

    @Column(name = "AGING_DAYS")
    private Integer agingDays;

    @Column(name = "REMARKS", length = 500)
    private String remarks;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
