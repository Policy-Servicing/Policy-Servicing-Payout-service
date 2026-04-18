package com.policyserve;

import com.policyserve.dto.AuditLogDTO;
import com.policyserve.entity.AuditManualProcessing;
import com.policyserve.repository.AuditManualProcessingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    @Autowired
    private AuditManualProcessingRepository auditRepository;

    @Value("${manual.processing.audit-enabled:true}")
    private boolean auditEnabled;

    @Transactional
    public void logAction(AuditLogDTO dto) {
        if (!auditEnabled) {
            log.debug("Audit logging disabled — skipping audit entry for policyNo={}", dto.getPolicyNo());
            return;
        }
        try {
            AuditManualProcessing entity = AuditManualProcessing.builder()
                .policyNo(dto.getPolicyNo())
                .requestId(dto.getRequestId())
                .actionTaken(dto.getActionTaken())
                .performedBy(dto.getPerformedBy())
                .remarks(dto.getRemarks())
                .status(dto.getStatus() != null ? dto.getStatus() : "SUCCESS")
                .performedAt(dto.getPerformedAt() != null ? dto.getPerformedAt() : new Date())
                .build();
            auditRepository.save(entity);
            log.info("Audit saved: policyNo={}, action={}, by={}", dto.getPolicyNo(), dto.getActionTaken(), dto.getPerformedBy());
        } catch (Exception ex) {
            // Audit failure must NEVER break business flow
            log.error("Failed to write audit log for policyNo={}: {}", dto.getPolicyNo(), ex.getMessage(), ex);
        }
    }
}
