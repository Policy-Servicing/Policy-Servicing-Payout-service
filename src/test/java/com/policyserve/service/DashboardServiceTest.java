package com.policyserve.service;

import com.policyserve.dto.*;
import com.policyserve.entity.PolicyTransaction;
import com.policyserve.repository.PolicyTransactionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DashboardServiceTest {

    @Mock
    private PolicyTransactionRepository txRepo;

    @InjectMocks
    private DashboardService dashboardService;

    // ── getModules ─────────────────────────────────────────────────────────────

    @Test
    public void getModules_shouldReturnFiveModules() {
        List<ModuleDto> modules = dashboardService.getModules();
        assertThat(modules).hasSize(5);
        assertThat(modules).extracting(ModuleDto::getCode)
                .containsExactlyInAnyOrder("SB", "MAT", "DEATH", "SUR", "LOAN");
    }

    // ── getProducts ────────────────────────────────────────────────────────────

    @Test
    public void getProducts_withValidModule_shouldReturnFilteredProducts() {
        List<ProductDto> products = dashboardService.getProducts("SB");
        assertThat(products).hasSize(3);
        assertThat(products).allMatch(p -> "SB".equals(p.getModuleCode()));
    }

    @Test
    public void getProducts_withNullModule_shouldReturnAllProducts() {
        List<ProductDto> products = dashboardService.getProducts(null);
        assertThat(products).hasSizeGreaterThan(3);
    }

    @Test
    public void getProducts_withUnknownModule_shouldReturnEmptyList() {
        List<ProductDto> products = dashboardService.getProducts("UNKNOWN");
        assertThat(products).isEmpty();
    }

    // ── getStageSummary ────────────────────────────────────────────────────────

    @Test
    public void getStageSummary_shouldReturnAllFiveStages() {
        when(txRepo.countByModuleCodeAndCurrentStage(anyString(), anyString())).thenReturn(10L);
        when(txRepo.countByModuleCodeAndCurrentStageAndAgingDaysGreaterThan(anyString(), anyString(), anyInt()))
                .thenReturn(2L);

        StageSummaryDto result = dashboardService.getStageSummary("SB", null, "TODAY");

        assertThat(result.getModule()).isEqualTo("SB");
        assertThat(result.getStageCounts()).hasSize(5);
        assertThat(result.getTotalPending()).isEqualTo(40L); // 4 non-PAID stages * 10 each
        assertThat(result.getSlaBreaches()).isEqualTo(8);    // 4 stages with agingCount=2 each
    }

    @Test
    public void getStageSummary_withProductId_shouldUseCorrectRepoMethod() {
        when(txRepo.countByModuleCodeAndProductIdAndCurrentStage(anyString(), anyString(), anyString()))
                .thenReturn(5L);
        when(txRepo.countByModuleCodeAndCurrentStageAndAgingDaysGreaterThan(anyString(), anyString(), anyInt()))
                .thenReturn(0L);

        StageSummaryDto result = dashboardService.getStageSummary("SB", "SB001", "TODAY");

        assertThat(result.getProductId()).isEqualTo("SB001");
        assertThat(result.getStageCounts()).allMatch(s -> s.getCount() == 5L);
    }

    // ── getDrillDown ───────────────────────────────────────────────────────────

    @Test
    public void getDrillDown_shouldReturnMappedRecords() {
        PolicyTransaction tx = new PolicyTransaction();
        tx.setPolicyNo("PLY-SB-001234");
        tx.setProductId("SB001");
        tx.setModuleCode("SB");
        tx.setHolderName("Ravi Kumar");
        tx.setCurrentStage("APPROVE");
        tx.setAmount(new BigDecimal("125000"));
        tx.setEnteredAt(LocalDateTime.now().minusDays(3));
        tx.setAgingDays(3);
        tx.setStatus("PENDING");

        when(txRepo.findByModuleCodeAndCurrentStage(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(tx)));

        DrillDownResponseDto result = dashboardService.getDrillDown("APPROVE", "SB", null, 0, 10);

        assertThat(result.getStage()).isEqualTo("APPROVE");
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getPolicyNo()).isEqualTo("PLY-SB-001234");
        assertThat(result.getTotalCount()).isEqualTo(1L);
    }

    @Test
    public void getDrillDown_emptyPage_shouldReturnEmptyList() {
        when(txRepo.findByModuleCodeAndCurrentStage(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        DrillDownResponseDto result = dashboardService.getDrillDown("PAID", "SB", null, 0, 10);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotalCount()).isEqualTo(0L);
    }

    // ── getTrend ───────────────────────────────────────────────────────────────

    @Test
    public void getTrend_emptyData_shouldReturnEmptyList() {
        when(txRepo.findTrendData(anyString(), anyInt())).thenReturn(Collections.emptyList());

        List<TrendPointDto> trend = dashboardService.getTrend("SB", 7);

        assertThat(trend).isEmpty();
    }

    @Test
    public void getTrend_withData_shouldGroupByDate() {
        Object[] row1 = new Object[]{"2024-04-10", "APPROVE", 25L};
        Object[] row2 = new Object[]{"2024-04-10", "PAID",    30L};
        Object[] row3 = new Object[]{"2024-04-11", "EXTRACT", 10L};

        when(txRepo.findTrendData(anyString(), anyInt()))
                .thenReturn(java.util.Arrays.asList(row1, row2, row3));

        List<TrendPointDto> trend = dashboardService.getTrend("SB", 7);

        assertThat(trend).hasSize(2);
        TrendPointDto day1 = trend.get(0);
        assertThat(day1.getApprove()).isEqualTo(25L);
        assertThat(day1.getPaid()).isEqualTo(30L);
    }
}
