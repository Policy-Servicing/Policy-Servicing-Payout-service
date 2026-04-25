package com.policyserve.service;

import com.policyserve.dto.*;
import com.policyserve.entity.PolicyTransaction;
import com.policyserve.repository.OracleDashboardRepository;
import com.policyserve.repository.PolicyTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private static final List<String> ALL_STAGES =
            Arrays.asList("EXTRACT", "APPROVE", "TECH_BUCKET", "SANCTION", "PAID");

    // Stage metadata: label, color, icon, agingThreshold (days)
    private static final Map<String, String[]> STAGE_META = new LinkedHashMap<>();
    static {
        STAGE_META.put("EXTRACT",     new String[]{"Extract",     "#6366F1", "download",   "3"});
        STAGE_META.put("APPROVE",     new String[]{"Approve",     "#F59E0B", "check_circle","2"});
        STAGE_META.put("TECH_BUCKET", new String[]{"Tech Bucket", "#8B5CF6", "settings",   "1"});
        STAGE_META.put("SANCTION",    new String[]{"Sanction",    "#EF4444", "gavel",       "1"});
        STAGE_META.put("PAID",        new String[]{"Paid",        "#10B981", "payments",    "0"});
    }

    /**
     * When true, stage counts and drill-down delegate to the Oracle DB package
     * (PKG_PS_DASHBOARD) via OracleDashboardRepository.
     * When false (default/dev), the existing JPA-based logic is used.
     * Toggle via application.properties: app.use-oracle-package=true
     */
    @Value("${app.use-oracle-package:false}")
    private boolean useOraclePackage;

    @Autowired
    private PolicyTransactionRepository txRepo;

    @Autowired
    private OracleDashboardRepository oracleRepo;

    // ── Modules ────────────────────────────────────────────────────────────────

    public List<ModuleDto> getModules() {
        return Arrays.asList(
            ModuleDto.builder().code("SB").name("Survival Benefit").description("Periodic survival benefit payouts").build(),
            ModuleDto.builder().code("MAT").name("Maturity").description("Policy maturity payouts").build(),
            ModuleDto.builder().code("DEATH").name("Death Claim").description("Death claim settlements").build(),
            ModuleDto.builder().code("SUR").name("Surrender").description("Policy surrender payouts").build(),
            ModuleDto.builder().code("LOAN").name("Loan").description("Policy loan disbursements").build()
        );
    }

    // ── Products ───────────────────────────────────────────────────────────────

    public List<ProductDto> getProducts(String module) {
        Map<String, List<ProductDto>> productMap = new HashMap<>();
        productMap.put("SB",    Arrays.asList(
            ProductDto.builder().id("SB001").name("Jeevan Anand – SB").moduleCode("SB").build(),
            ProductDto.builder().id("SB002").name("Jeevan Umang – SB").moduleCode("SB").build(),
            ProductDto.builder().id("SB003").name("New Endowment – SB").moduleCode("SB").build()
        ));
        productMap.put("MAT",   Arrays.asList(
            ProductDto.builder().id("MAT001").name("Jeevan Anand – Mat").moduleCode("MAT").build(),
            ProductDto.builder().id("MAT002").name("Jeevan Labh – Mat").moduleCode("MAT").build()
        ));
        productMap.put("DEATH", Arrays.asList(
            ProductDto.builder().id("DC001").name("Term Assurance").moduleCode("DEATH").build(),
            ProductDto.builder().id("DC002").name("Whole Life").moduleCode("DEATH").build()
        ));
        productMap.put("SUR",   Arrays.asList(
            ProductDto.builder().id("SUR001").name("Jeevan Anand – Sur").moduleCode("SUR").build()
        ));
        productMap.put("LOAN",  Arrays.asList(
            ProductDto.builder().id("LN001").name("Policy Loan").moduleCode("LOAN").build()
        ));

        if (module == null || module.isEmpty()) {
            return productMap.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
        return productMap.getOrDefault(module.toUpperCase(), Collections.emptyList());
    }

    // ── Stage Summary ──────────────────────────────────────────────────────────

    public StageSummaryDto getStageSummary(String module, String productId, String dateRange) {
        log.info("getStageSummary: module={}, productId={}, dateRange={}, useOraclePackage={}",
                module, productId, dateRange, useOraclePackage);

        // ── Oracle Package path ─────────────────────────────────────────────
        if (useOraclePackage) {
            List<Map<String, Object>> oracleRows = oracleRepo.getStageCounts(module);
            List<StageCountDto> stageCounts = oracleRows.stream().map(row -> {
                String stage = String.valueOf(row.getOrDefault("stage_code", ""));
                Number recCount = (Number) row.getOrDefault("rec_count", 0);
                String[] meta = STAGE_META.getOrDefault(stage, new String[]{stage, "#6366F1", "info", "0"});
                long count = recCount != null ? recCount.longValue() : 0L;
                return StageCountDto.builder()
                        .stage(stage)
                        .label(String.valueOf(row.getOrDefault("stage_label", stage)))
                        .color(meta[1])
                        .icon(meta[2])
                        .count(count)
                        .previousCount(count > 0 ? (long)(count * 0.85) : 0L)
                        .agingCount(0)
                        .agingThresholdDays(0)
                        .build();
            }).collect(Collectors.toList());

            long totalPending = stageCounts.stream()
                    .filter(s -> !"PAID".equalsIgnoreCase(s.getStage()))
                    .mapToLong(StageCountDto::getCount).sum();
            return StageSummaryDto.builder()
                    .module(module).productId(productId).dateRange(dateRange)
                    .stageCounts(stageCounts).totalPending(totalPending)
                    .totalAmount(null).slaBreaches(0).lastUpdated(LocalDateTime.now())
                    .build();
        }

        // ── JPA fallback path (dev / no Oracle DB) ─────────────────────────
        List<StageCountDto> stageCounts = ALL_STAGES.stream().map(stage -> {
            String[] meta = STAGE_META.get(stage);
            int threshold = Integer.parseInt(meta[3]);

            long count = (productId != null && !productId.isEmpty())
                    ? txRepo.countByModuleCodeAndProductIdAndCurrentStage(module, productId, stage)
                    : txRepo.countByModuleCodeAndCurrentStage(module, stage);

            int agingCount = (threshold > 0)
                    ? (int) txRepo.countByModuleCodeAndCurrentStageAndAgingDaysGreaterThan(module, stage, threshold)
                    : 0;

            return StageCountDto.builder()
                    .stage(stage)
                    .label(meta[0])
                    .color(meta[1])
                    .icon(meta[2])
                    .count(count)
                    .previousCount(count > 0 ? (long) (count * 0.85) : 0L)
                    .agingCount(agingCount)
                    .agingThresholdDays(threshold)
                    .build();
        }).collect(Collectors.toList());

        long totalPending = stageCounts.stream()
                .filter(s -> !s.getStage().equals("PAID"))
                .mapToLong(StageCountDto::getCount).sum();
        int slaBreaches = stageCounts.stream()
                .mapToInt(s -> s.getAgingCount() != null ? s.getAgingCount() : 0).sum();

        return StageSummaryDto.builder()
                .module(module).productId(productId).dateRange(dateRange)
                .stageCounts(stageCounts).totalPending(totalPending)
                .totalAmount(null).slaBreaches(slaBreaches).lastUpdated(LocalDateTime.now())
                .build();
    }

    // ── Drill Down ─────────────────────────────────────────────────────────────

    public DrillDownResponseDto getDrillDown(String stage, String module,
                                             String productId, int page, int size) {
        log.info("getDrillDown: stage={}, module={}, productId={}, page={}, size={}, useOraclePackage={}",
                stage, module, productId, page, size, useOraclePackage);

        // ── Oracle Package path ─────────────────────────────────────────────
        if (useOraclePackage) {
            // Oracle pages from 1; Spring pages from 0 — convert
            Map<String, Object> oracleResult = oracleRepo.getStageDrilldown(module, stage, page + 1, size);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawRecords =
                    (List<Map<String, Object>>) oracleResult.getOrDefault("records", Collections.emptyList());

            List<PolicyRecordDto> records = rawRecords.stream().map(row ->
                PolicyRecordDto.builder()
                    .policyNo(str(row, "policy_no"))
                    .productId(str(row, "product_id"))
                    .moduleCode(module)
                    .holderName(str(row, "holder_name"))
                    .stage(stage)
                    .amount(row.get("amount") instanceof Number
                            ? new java.math.BigDecimal(row.get("amount").toString()) : null)
                    .enteredAt(null)   // date columns returned as String; map as needed
                    .agingDays(null)
                    .status(str(row, "status"))
                    .remarks(str(row, "remarks"))
                    .build()
            ).collect(Collectors.toList());

            long total = ((Number) oracleResult.getOrDefault("totalCount", 0L)).longValue();
            return DrillDownResponseDto.builder()
                    .stage(stage).records(records).totalCount(total).page(page).size(size)
                    .build();
        }

        // ── JPA fallback path ──────────────────────────────────────────────
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "enteredAt"));

        Page<PolicyTransaction> resultPage = (productId != null && !productId.isEmpty())
                ? txRepo.findByModuleCodeAndProductIdAndCurrentStage(module, productId, stage, pageable)
                : txRepo.findByModuleCodeAndCurrentStage(module, stage, pageable);

        List<PolicyRecordDto> records = resultPage.getContent().stream().map(tx ->
            PolicyRecordDto.builder()
                .policyNo(tx.getPolicyNo())
                .productId(tx.getProductId())
                .moduleCode(tx.getModuleCode())
                .holderName(tx.getHolderName())
                .stage(tx.getCurrentStage())
                .amount(tx.getAmount())
                .enteredAt(tx.getEnteredAt())
                .agingDays(tx.getAgingDays())
                .status(tx.getStatus())
                .remarks(tx.getRemarks())
                .build()
        ).collect(Collectors.toList());

        return DrillDownResponseDto.builder()
                .stage(stage).records(records)
                .totalCount(resultPage.getTotalElements()).page(page).size(size)
                .build();
    }

    // ── Audit Logs (Oracle only) ─────────────────────────────────────────────

    /**
     * Returns recent PS_API_CALL_LOG entries from the Oracle DB.
     * Only meaningful when useOraclePackage=true.
     */
    public List<Map<String, Object>> getAuditLogs(String moduleCode, int limit) {
        if (!useOraclePackage) {
            log.warn("getAuditLogs called but app.use-oracle-package=false — returning empty list");
            return Collections.emptyList();
        }
        return oracleRepo.getAuditLogs(moduleCode, limit);
    }

    // ── Private Helper ──────────────────────────────────────────────────────

    private String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v != null ? v.toString() : null;
    }

    // ── Trend ─────────────────────────────────────────────────────────────────

    public List<TrendPointDto> getTrend(String module, int days) {
        log.info("getTrend: module={}, days={}", module, days);

        List<Object[]> rawRows = txRepo.findTrendData(module, days);

        // Group by date, then pivot stages into columns
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
        Map<String, TrendPointDto> byDate = new LinkedHashMap<>();

        for (Object[] row : rawRows) {
            // row[0] = run_date (java.sql.Date or Timestamp), row[1] = stage, row[2] = count
            String dateKey = row[0] != null ? row[0].toString().substring(0, 10) : "unknown";
            String stage   = row[1] != null ? row[1].toString() : "";
            long   cnt     = row[2] != null ? ((Number) row[2]).longValue() : 0L;

            TrendPointDto point = byDate.computeIfAbsent(dateKey, k ->
                    TrendPointDto.builder().date(k)
                            .extract(0L).approve(0L).techBucket(0L).sanction(0L).paid(0L).build());

            switch (stage) {
                case "EXTRACT":     point.setExtract(cnt);     break;
                case "APPROVE":     point.setApprove(cnt);     break;
                case "TECH_BUCKET": point.setTechBucket(cnt);  break;
                case "SANCTION":    point.setSanction(cnt);    break;
                case "PAID":        point.setPaid(cnt);        break;
            }
        }

        return new ArrayList<>(byDate.values());
    }
}
