-- =============================================================
-- FILE: 03_package_spec.sql
-- PURPOSE: PKG_PS_DASHBOARD — Package specification
-- DB: Oracle 19c | Schema: CUST
-- RUN ORDER: 3rd
-- =============================================================

CREATE OR REPLACE PACKAGE CUST.PKG_PS_DASHBOARD AS
  /*
   * Policy Servicing Dashboard — DB Package
   * Exposes stage count queries, paginated drill-down,
   * and flexi accumulation detail for Spring Boot consumption.
   *
   * Version  : 1.0
   * Author   : Policy Servicing Team
   * Created  : 2026-04-25
   * DB       : Oracle 19c
   */

  -- ────────────────────────────────────────────────────────
  -- P1: GET_MODULE_LIST
  --   Returns module codes and display names for dropdowns.
  --   OUT: P_RESULT  SYS_REFCURSOR
  --         Columns: MODULE_CODE VARCHAR2, MODULE_NAME VARCHAR2
  -- ────────────────────────────────────────────────────────
  PROCEDURE GET_MODULE_LIST (
    P_RESULT  OUT SYS_REFCURSOR
  );

  -- ────────────────────────────────────────────────────────
  -- P2: GET_STAGE_COUNTS
  --   Returns stage-wise record counts.
  --   P_MODULE_CODE = NULL → all modules.
  --   OUT: P_RESULT  SYS_REFCURSOR
  --         Columns: MODULE_CODE, MODULE_NAME, STAGE_CODE,
  --                  STAGE_LABEL, SORT_ORDER, REC_COUNT
  -- ────────────────────────────────────────────────────────
  PROCEDURE GET_STAGE_COUNTS (
    P_MODULE_CODE  IN  VARCHAR2 DEFAULT NULL,
    P_RESULT       OUT SYS_REFCURSOR
  );

  -- ────────────────────────────────────────────────────────
  -- P3: GET_STAGE_DRILLDOWN
  --   Returns paginated records for a specific module + stage.
  --   Uses OFFSET/FETCH (Oracle 19c).
  --   OUT: P_TOTAL_COUNT  Total matching rows (for pagination)
  --        P_RESULT       SYS_REFCURSOR of detail rows
  -- ────────────────────────────────────────────────────────
  PROCEDURE GET_STAGE_DRILLDOWN (
    P_MODULE_CODE   IN  VARCHAR2,
    P_STAGE_CODE    IN  VARCHAR2,
    P_PAGE_NO       IN  NUMBER   DEFAULT 1,
    P_PAGE_SIZE     IN  NUMBER   DEFAULT 20,
    P_TOTAL_COUNT   OUT NUMBER,
    P_RESULT        OUT SYS_REFCURSOR
  );

  -- ────────────────────────────────────────────────────────
  -- P4: GET_FLEXI_ACCUM_DETAIL
  --   Returns AZBJ_FLEXI_ACCUM_DETAIL rows for a policy.
  --   Uses existing WEO_REC_STRINGS50 object type.
  --   OUT: P_RESULT  CUST.weo_rec_strings50_list
  -- ────────────────────────────────────────────────────────
  PROCEDURE GET_FLEXI_ACCUM_DETAIL (
    P_POLICY_REF  IN  VARCHAR2,
    P_RESULT      OUT CUST.weo_rec_strings50_list
  );

  -- ────────────────────────────────────────────────────────
  -- P5: GET_AUDIT_LOGS
  --   Returns recent procedure call logs for monitoring.
  --   OUT: P_RESULT  SYS_REFCURSOR
  -- ────────────────────────────────────────────────────────
  PROCEDURE GET_AUDIT_LOGS (
    P_MODULE_CODE  IN  VARCHAR2 DEFAULT NULL,
    P_LIMIT        IN  NUMBER   DEFAULT 100,
    P_RESULT       OUT SYS_REFCURSOR
  );

END PKG_PS_DASHBOARD;
/
