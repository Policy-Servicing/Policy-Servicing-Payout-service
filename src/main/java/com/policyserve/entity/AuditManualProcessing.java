package com.policyserve.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * JPA entity for AUDIT_MANUAL_PROCESSING Oracle table.
 */
@Entity
@Table(name = "AUDIT_MANUAL_PROCESSING")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditManualProcessing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_ID")
    private Long auditId;

    @Column(name = "POLICY_NO", nullable = false, length = 50)
    private String policyNo;

    @Column(name = "REQUEST_ID", nullable = false, length = 50)
    private String requestId;

    @Column(name = "ACTION_TAKEN", nullable = false, length = 20)
    private String actionTaken;

    @Column(name = "PERFORMED_BY", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "REMARKS", length = 200)
    private String remarks;

    @CreationTimestamp
    @Column(name = "PERFORMED_AT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date performedAt;

    @Column(name = "STATUS", length = 20)
    private String status;
}
