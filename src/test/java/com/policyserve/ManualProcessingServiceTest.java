package com.policyserve;

import com.policyserve.dto.PolicyDetailDTO;
import com.policyserve.dto.ProcessResponseDTO;
import com.policyserve.exception.PolicyNotFoundException;
import com.policyserve.repository.ManualProcessingRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ManualProcessingServiceTest {

    @Mock
    private ManualProcessingRepository repository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ManualProcessingService manualProcessingService;

    // ── lookupPolicy ───────────────────────────────────────────────────────────

    @Test
    public void lookupPolicy_found_shouldReturnPolicyDetail() {
        Object[] row = new Object[]{
            "PLY-SB-001234", "REQ-2024-56789", "Survival Benefit",
            "Ravi Kumar", "SB001", "APPROVE",
            new BigDecimal("125000.00"), null, null
        };
        Object[] historyRow = new Object[]{"EXTRACT", "COMPLETED", null, null};

        when(repository.findPolicyTransaction("PLY-SB-001234", "REQ-2024-56789"))
                .thenReturn(Collections.singletonList(row));
        when(repository.findStageHistory("PLY-SB-001234", "REQ-2024-56789"))
                .thenReturn(Collections.singletonList(historyRow));

        PolicyDetailDTO result = manualProcessingService
                .lookupPolicy("PLY-SB-001234", "REQ-2024-56789");

        assertThat(result.getPolicyNo()).isEqualTo("PLY-SB-001234");
        assertThat(result.getCustomerName()).isEqualTo("Ravi Kumar");
        assertThat(result.getCurrentStage()).isEqualTo("APPROVE");
        assertThat(result.getStageHistory()).hasSize(1);
        assertThat(result.getStageHistory().get(0).getStageName()).isEqualTo("EXTRACT");
    }

    @Test
    public void lookupPolicy_notFound_shouldThrowPolicyNotFoundException() {
        when(repository.findPolicyTransaction(anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> manualProcessingService
                .lookupPolicy("PLY-UNKNOWN", "REQ-UNKNOWN"))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    public void lookupPolicy_withMultipleHistoryStages_shouldMapAll() {
        Object[] row = new Object[]{
            "PLY-SB-001234", "REQ-2024-56789", "Survival Benefit",
            "Ravi Kumar", "SB001", "SANCTION",
            new BigDecimal("250000.00"), null, null
        };
        List<Object[]> historyRows = Arrays.asList(
            new Object[]{"EXTRACT",  "COMPLETED", null, null},
            new Object[]{"APPROVE",  "COMPLETED", null, null},
            new Object[]{"SANCTION", "CURRENT",   null, null}
        );

        when(repository.findPolicyTransaction(anyString(), anyString()))
                .thenReturn(Collections.singletonList(row));
        when(repository.findStageHistory(anyString(), anyString()))
                .thenReturn(historyRows);

        PolicyDetailDTO result = manualProcessingService
                .lookupPolicy("PLY-SB-001234", "REQ-2024-56789");

        assertThat(result.getStageHistory()).hasSize(3);
        assertThat(result.getCurrentStage()).isEqualTo("SANCTION");
    }

    // ── processPolicy ─────────────────────────────────────────────────────────

    @Test
    public void processPolicy_success_shouldReturnSuccessResponse() {
        Query mockQuery = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.executeUpdate()).thenReturn(1);

        ProcessResponseDTO result = manualProcessingService
                .processPolicy("PLY-SB-001234", "REQ-2024-56789", "APPROVE", "Verified manually");

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getNewStage()).isEqualTo("PAID");
        assertThat(result.getProcessedAt()).isNotNull();
    }
}
