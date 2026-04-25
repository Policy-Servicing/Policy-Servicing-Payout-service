package com.policyserve.repository;

import com.policyserve.exception.ProcedureExecutionException;
import oracle.jdbc.OracleTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * Oracle DB bridge for PKG_PS_DASHBOARD stored procedures.
 *
 * This repository sits ALONGSIDE the existing PolicyTransactionRepository (JPA).
 * DashboardService selects between them via the "app.use-oracle-package" flag.
 *
 * Package: CUST.PKG_PS_DASHBOARD
 * Procedures wrapped:
 *   - GET_MODULE_LIST
 *   - GET_STAGE_COUNTS
 *   - GET_STAGE_DRILLDOWN
 *   - GET_FLEXI_ACCUM_DETAIL
 *   - GET_AUDIT_LOGS
 */
@Repository
public class OracleDashboardRepository {

    private static final Logger log = LoggerFactory.getLogger(OracleDashboardRepository.class);

    private static final String CATALOG = "PKG_PS_DASHBOARD";

    private final SimpleJdbcCall moduleListCall;
    private final SimpleJdbcCall stageCountsCall;
    private final SimpleJdbcCall stageDrilldownCall;
    private final SimpleJdbcCall flexiAccumCall;
    private final SimpleJdbcCall auditLogsCall;

    public OracleDashboardRepository(DataSource dataSource) {

        // GET_MODULE_LIST → cursor of (MODULE_CODE, MODULE_NAME)
        this.moduleListCall = new SimpleJdbcCall(dataSource)
            .withCatalogName(CATALOG)
            .withProcedureName("GET_MODULE_LIST")
            .declareParameters(
                new SqlOutParameter("P_RESULT", OracleTypes.CURSOR,
                    (rs, rowNum) -> rowToMap(rs))
            );

        // GET_STAGE_COUNTS → cursor of stage summary rows
        this.stageCountsCall = new SimpleJdbcCall(dataSource)
            .withCatalogName(CATALOG)
            .withProcedureName("GET_STAGE_COUNTS")
            .declareParameters(
                new SqlParameter("P_MODULE_CODE", Types.VARCHAR),
                new SqlOutParameter("P_RESULT", OracleTypes.CURSOR,
                    (rs, rowNum) -> rowToMap(rs))
            );

        // GET_STAGE_DRILLDOWN → paginated records + total count
        this.stageDrilldownCall = new SimpleJdbcCall(dataSource)
            .withCatalogName(CATALOG)
            .withProcedureName("GET_STAGE_DRILLDOWN")
            .declareParameters(
                new SqlParameter("P_MODULE_CODE", Types.VARCHAR),
                new SqlParameter("P_STAGE_CODE",  Types.VARCHAR),
                new SqlParameter("P_PAGE_NO",     Types.NUMERIC),
                new SqlParameter("P_PAGE_SIZE",   Types.NUMERIC),
                new SqlOutParameter("P_TOTAL_COUNT", Types.NUMERIC),
                new SqlOutParameter("P_RESULT", OracleTypes.CURSOR,
                    (rs, rowNum) -> rowToMap(rs))
            );

        // GET_FLEXI_ACCUM_DETAIL → weo_rec_strings50_list (mapped to list of maps)
        this.flexiAccumCall = new SimpleJdbcCall(dataSource)
            .withCatalogName(CATALOG)
            .withProcedureName("GET_FLEXI_ACCUM_DETAIL")
            .declareParameters(
                new SqlParameter("P_POLICY_REF", Types.VARCHAR),
                new SqlOutParameter("P_RESULT", OracleTypes.CURSOR,
                    (rs, rowNum) -> rowToMap(rs))
            );

        // GET_AUDIT_LOGS → recent call log rows
        this.auditLogsCall = new SimpleJdbcCall(dataSource)
            .withCatalogName(CATALOG)
            .withProcedureName("GET_AUDIT_LOGS")
            .declareParameters(
                new SqlParameter("P_MODULE_CODE", Types.VARCHAR),
                new SqlParameter("P_LIMIT",       Types.NUMERIC),
                new SqlOutParameter("P_RESULT", OracleTypes.CURSOR,
                    (rs, rowNum) -> rowToMap(rs))
            );
    }

    // ─────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────

    /**
     * Returns list of module codes + display names.
     * Maps to: GET /api/v1/dashboard/modules
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getModuleList() {
        try {
            Map<String, Object> out = moduleListCall.execute(new MapSqlParameterSource());
            return (List<Map<String, Object>>) out.getOrDefault("P_RESULT", Collections.emptyList());
        } catch (Exception ex) {
            log.error("GET_MODULE_LIST failed: {}", ex.getMessage(), ex);
            throw new ProcedureExecutionException("GET_MODULE_LIST failed: " + ex.getMessage());
        }
    }

    /**
     * Returns stage-wise record counts. Pass null for all modules.
     * Maps to: GET /api/v1/dashboard/stage-counts
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getStageCounts(String moduleCode) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("P_MODULE_CODE", moduleCode, Types.VARCHAR);
            Map<String, Object> out = stageCountsCall.execute(params);
            return (List<Map<String, Object>>) out.getOrDefault("P_RESULT", Collections.emptyList());
        } catch (Exception ex) {
            log.error("GET_STAGE_COUNTS failed: module={}, error={}", moduleCode, ex.getMessage(), ex);
            throw new ProcedureExecutionException("GET_STAGE_COUNTS failed: " + ex.getMessage());
        }
    }

    /**
     * Returns paginated drill-down records for a specific module + stage.
     * Maps to: GET /api/v1/dashboard/stage-drill
     *
     * @return Map with keys: "records" (List), "totalCount" (Long)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStageDrilldown(String moduleCode, String stageCode,
                                                   int pageNo, int pageSize) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("P_MODULE_CODE", moduleCode, Types.VARCHAR)
                .addValue("P_STAGE_CODE",  stageCode,  Types.VARCHAR)
                .addValue("P_PAGE_NO",     pageNo,     Types.NUMERIC)
                .addValue("P_PAGE_SIZE",   pageSize,   Types.NUMERIC);

            Map<String, Object> out = stageDrilldownCall.execute(params);

            List<Map<String, Object>> records =
                (List<Map<String, Object>>) out.getOrDefault("P_RESULT", Collections.emptyList());
            Number totalCount = (Number) out.get("P_TOTAL_COUNT");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("records",    records);
            result.put("totalCount", totalCount != null ? totalCount.longValue() : 0L);
            result.put("page",       pageNo);
            result.put("pageSize",   pageSize);
            result.put("totalPages", pageSize > 0
                ? (int) Math.ceil((double)(totalCount != null ? totalCount.longValue() : 0) / pageSize)
                : 0);
            return result;

        } catch (Exception ex) {
            log.error("GET_STAGE_DRILLDOWN failed: module={}, stage={}, error={}",
                moduleCode, stageCode, ex.getMessage(), ex);
            throw new ProcedureExecutionException("GET_STAGE_DRILLDOWN failed: " + ex.getMessage());
        }
    }

    /**
     * Returns flexi accumulation detail for a policy reference.
     * Uses the existing WEO_REC_STRINGS50 Oracle type.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFlexiAccumDetail(String policyRef) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("P_POLICY_REF", policyRef, Types.VARCHAR);
            Map<String, Object> out = flexiAccumCall.execute(params);
            return (List<Map<String, Object>>) out.getOrDefault("P_RESULT", Collections.emptyList());
        } catch (Exception ex) {
            log.error("GET_FLEXI_ACCUM_DETAIL failed: policyRef={}, error={}", policyRef, ex.getMessage(), ex);
            throw new ProcedureExecutionException("GET_FLEXI_ACCUM_DETAIL failed: " + ex.getMessage());
        }
    }

    /**
     * Returns recent procedure call audit logs from PS_API_CALL_LOG.
     * Maps to: GET /api/v1/dashboard/audit-logs
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAuditLogs(String moduleCode, int limit) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("P_MODULE_CODE", moduleCode, Types.VARCHAR)
                .addValue("P_LIMIT",       limit,      Types.NUMERIC);
            Map<String, Object> out = auditLogsCall.execute(params);
            return (List<Map<String, Object>>) out.getOrDefault("P_RESULT", Collections.emptyList());
        } catch (Exception ex) {
            log.error("GET_AUDIT_LOGS failed: module={}, error={}", moduleCode, ex.getMessage(), ex);
            throw new ProcedureExecutionException("GET_AUDIT_LOGS failed: " + ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────

    /**
     * Maps a ResultSet row to a LinkedHashMap (column order preserved).
     * Column names are lowercased for consistent JSON serialization.
     */
    private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        int colCount = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= colCount; i++) {
            String colName = rs.getMetaData().getColumnLabel(i).toLowerCase();
            row.put(colName, rs.getObject(i));
        }
        return row;
    }
}
