package com.policyserve;

import com.policyserve.dto.PolicyDetailDTO;
import com.policyserve.dto.ProcessResponseDTO;
import com.policyserve.dto.StageHistoryDTO;
import com.policyserve.exception.PolicyNotFoundException;
import com.policyserve.exception.ProcedureExecutionException;
import com.policyserve.repository.ManualProcessingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ManualProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ManualProcessingService.class);

    @Autowired
    private ManualProcessingRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    // ── Lookup ────────────────────────────────────────────────────────────────

    public PolicyDetailDTO lookupPolicy(String policyNo, String requestId) {
        log.info("Looking up policy: policyNo={}, requestId={}", policyNo, requestId);

        List<Object[]> rows = repository.findPolicyTransaction(policyNo, requestId);
        if (rows == null || rows.isEmpty()) {
            throw new PolicyNotFoundException(policyNo, requestId);
        }

        Object[] row = rows.get(0);
        List<Object[]> historyRows = repository.findStageHistory(policyNo, requestId);

        List<StageHistoryDTO> stageHistory = new ArrayList<>();
        for (Object[] h : historyRows) {
            StageHistoryDTO dto = StageHistoryDTO.builder()
                .stageName(asString(h[0]))
                .status(asString(h[1]))
                .completedAt(asDate(h[2]))
                .entryDate(asDate(h[3]))
                .build();
            stageHistory.add(dto);
        }

        return PolicyDetailDTO.builder()
            .policyNo(asString(row[0]))
            .requestId(asString(row[1]))
            .moduleName(asString(row[2]))
            .customerName(asString(row[3]))
            .productId(asString(row[4]))
            .currentStage(asString(row[5]))
            .payoutAmount(asBigDecimal(row[6]))
            .dateOfCommencement(asDate(row[7]))
            .maturityDate(asDate(row[8]))
            .stageHistory(stageHistory)
            .build();
    }

    // ── Process ───────────────────────────────────────────────────────────────

    @Transactional
    public ProcessResponseDTO processPolicy(String policyNo, String requestId,
                                            String action, String remarks) {
        log.info("Processing policy: policyNo={}, requestId={}, action={}", policyNo, requestId, action);
        try {
            callStoredProcedure(policyNo, requestId);
        } catch (Exception ex) {
            log.error("Stored procedure failed for policyNo={}: {}", policyNo, ex.getMessage(), ex);
            throw new ProcedureExecutionException(
                "Oracle procedure error: " + ex.getMessage(), ex);
        }

        return ProcessResponseDTO.builder()
            .status("SUCCESS")
            .message("Policy " + policyNo + " processed via auto_approve_validation.")
            .newStage("PAID")
            .processedAt(new Date())
            .build();
    }

    @Transactional
    public void callStoredProcedure(String policyNo, String requestId) {
        Query query = entityManager.createNativeQuery(
            "BEGIN customer.azbj_pkg_sb.auto_approve_validation(:policyNo, :requestId); END;"
        );
        query.setParameter("policyNo", policyNo);
        query.setParameter("requestId", requestId);
        query.executeUpdate();
    }

    // ── Type-safe helpers for raw JDBC Object[] rows ──────────────────────────

    private String asString(Object val) {
        return val != null ? val.toString() : null;
    }

    private BigDecimal asBigDecimal(Object val) {
        if (val == null) { return null; }
        if (val instanceof BigDecimal) { return (BigDecimal) val; }
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }

    private Date asDate(Object val) {
        if (val == null) { return null; }
        if (val instanceof Date) { return (Date) val; }
        if (val instanceof java.sql.Date) { return new Date(((java.sql.Date) val).getTime()); }
        if (val instanceof java.sql.Timestamp) { return new Date(((java.sql.Timestamp) val).getTime()); }
        return null;
    }
}
