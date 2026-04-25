-- =============================================================
-- FILE: 04_package_body.sql
-- PURPOSE: PKG_PS_DASHBOARD — Package body implementation
-- DB: Oracle 19c | Schema: CUST
-- RUN ORDER: 4th (after 03_package_spec.sql compiles successfully)
-- =============================================================

CREATE OR REPLACE PACKAGE BODY CUST.PKG_PS_DASHBOARD AS

  -- ============================================================
  -- PRIVATE: LOG_CALL
  --   Autonomous transaction — writes to PS_API_CALL_LOG without
  --   interfering with the caller's transaction context.
  --   Never raises exceptions (logging must not break main flow).
  -- ============================================================
  PROCEDURE LOG_CALL (
    P_MODULE   IN VARCHAR2,
    P_PROC     IN VARCHAR2,
    P_STATUS   IN VARCHAR2,
    P_ERR_CODE IN VARCHAR2 DEFAULT NULL,
    P_ERR_MSG  IN VARCHAR2 DEFAULT NULL,
    P_MS       IN NUMBER   DEFAULT 0
  ) IS
    PRAGMA AUTONOMOUS_TRANSACTION;
  BEGIN
    INSERT INTO CUST.PS_API_CALL_LOG (
      MODULE_CODE, PROC_NAME, STATUS, ERR_CODE, ERR_MSG, EXEC_MS
    ) VALUES (
      P_MODULE, P_PROC, P_STATUS,
      SUBSTR(P_ERR_CODE, 1, 20),
      SUBSTR(P_ERR_MSG,  1, 4000),
      P_MS
    );
    COMMIT;
  EXCEPTION
    WHEN OTHERS THEN NULL; -- Never let logging break the main call
  END LOG_CALL;

  -- ============================================================
  -- PRIVATE: ELAPSED_MS
  --   Returns milliseconds elapsed since P_START timestamp.
  -- ============================================================
  FUNCTION ELAPSED_MS (P_START IN TIMESTAMP) RETURN NUMBER IS
    V_DIFF INTERVAL DAY(9) TO SECOND(6);
  BEGIN
    V_DIFF := SYSTIMESTAMP - P_START;
    RETURN   EXTRACT(DAY    FROM V_DIFF) * 86400000
           + EXTRACT(HOUR   FROM V_DIFF) * 3600000
           + EXTRACT(MINUTE FROM V_DIFF) * 60000
           + ROUND(EXTRACT(SECOND FROM V_DIFF) * 1000);
  END ELAPSED_MS;

  -- ============================================================
  -- P1: GET_MODULE_LIST
  --   Returns the distinct module codes + human-readable names
  --   from the unified stage summary view.
  -- ============================================================
  PROCEDURE GET_MODULE_LIST (
    P_RESULT OUT SYS_REFCURSOR
  ) IS
    V_START TIMESTAMP := SYSTIMESTAMP;
  BEGIN
    OPEN P_RESULT FOR
      SELECT MODULE_CODE,
             CASE MODULE_CODE
               WHEN 'SB'   THEN 'Survival Benefit'
               WHEN 'ANN'  THEN 'Annuity'
               WHEN 'MAT'  THEN 'Maturity'
               WHEN 'REF'  THEN 'Refund'
               WHEN 'SUR'  THEN 'Surrender'
               WHEN 'LOAN' THEN 'Loan'
               ELSE MODULE_CODE
             END AS MODULE_NAME
        FROM (
          SELECT DISTINCT MODULE_CODE
            FROM CUST.V_PS_STAGE_SUMMARY
           ORDER BY MODULE_CODE
        );

    LOG_CALL(NULL, 'GET_MODULE_LIST', 'SUCCESS',
             NULL, NULL, ELAPSED_MS(V_START));
  EXCEPTION
    WHEN OTHERS THEN
      LOG_CALL(NULL, 'GET_MODULE_LIST', 'ERROR',
               TO_CHAR(SQLCODE), SQLERRM, 0);
      RAISE;
  END GET_MODULE_LIST;

  -- ============================================================
  -- P2: GET_STAGE_COUNTS
  --   Returns all stage-wise counts from V_PS_STAGE_SUMMARY.
  --   Pass P_MODULE_CODE = NULL to get all modules at once.
  --   Ordered by MODULE_CODE, SORT_ORDER for consistent display.
  -- ============================================================
  PROCEDURE GET_STAGE_COUNTS (
    P_MODULE_CODE IN  VARCHAR2 DEFAULT NULL,
    P_RESULT      OUT SYS_REFCURSOR
  ) IS
    V_START TIMESTAMP := SYSTIMESTAMP;
  BEGIN
    OPEN P_RESULT FOR
      SELECT MODULE_CODE,
             MODULE_NAME,
             STAGE_CODE,
             STAGE_LABEL,
             SORT_ORDER,
             REC_COUNT
        FROM CUST.V_PS_STAGE_SUMMARY
       WHERE (P_MODULE_CODE IS NULL
              OR MODULE_CODE = P_MODULE_CODE)
       ORDER BY MODULE_CODE, SORT_ORDER;

    LOG_CALL(P_MODULE_CODE, 'GET_STAGE_COUNTS', 'SUCCESS',
             NULL, NULL, ELAPSED_MS(V_START));
  EXCEPTION
    WHEN OTHERS THEN
      LOG_CALL(P_MODULE_CODE, 'GET_STAGE_COUNTS', 'ERROR',
               TO_CHAR(SQLCODE), SQLERRM, 0);
      RAISE;
  END GET_STAGE_COUNTS;

  -- ============================================================
  -- P3: GET_STAGE_DRILLDOWN
  --   Paginated detail records for a specific module + stage.
  --   Oracle 19c OFFSET/FETCH used for server-side pagination.
  --   P_PAGE_NO is 1-based (Spring passes page+1).
  --
  --   IMPORTANT: Replace placeholder column names (policy_no,
  --   customer_name, etc.) with your actual table column names.
  --   Use: SELECT column_name FROM all_tab_columns
  --        WHERE table_name='AZBJ_SURVIVAL_PROCESSING' AND owner='CUST';
  -- ============================================================
  PROCEDURE GET_STAGE_DRILLDOWN (
    P_MODULE_CODE   IN  VARCHAR2,
    P_STAGE_CODE    IN  VARCHAR2,
    P_PAGE_NO       IN  NUMBER   DEFAULT 1,
    P_PAGE_SIZE     IN  NUMBER   DEFAULT 20,
    P_TOTAL_COUNT   OUT NUMBER,
    P_RESULT        OUT SYS_REFCURSOR
  ) IS
    V_START  TIMESTAMP := SYSTIMESTAMP;
    V_OFFSET NUMBER    := (P_PAGE_NO - 1) * P_PAGE_SIZE;
  BEGIN
    P_TOTAL_COUNT := 0;

    -- ── Survival Benefit ──────────────────────────────────────
    IF P_MODULE_CODE = 'SB' THEN

      SELECT COUNT(*) INTO P_TOTAL_COUNT
        FROM CUST.azbj_survival_processing
       WHERE OPS_auth_flg = P_STAGE_CODE;

      OPEN P_RESULT FOR
        SELECT policy_no,
               customer_name,        -- TODO: verify column name
               product_code,         -- TODO: verify column name
               OPS_auth_flg          AS stage_code,
               benefit_amount,       -- TODO: verify column name
               due_date,             -- TODO: verify column name
               processed_date,       -- TODO: verify column name
               remarks               -- TODO: verify column name
          FROM CUST.azbj_survival_processing
         WHERE OPS_auth_flg = P_STAGE_CODE
         ORDER BY due_date DESC
         OFFSET V_OFFSET ROWS FETCH NEXT P_PAGE_SIZE ROWS ONLY;

    -- ── Annuity ───────────────────────────────────────────────
    ELSIF P_MODULE_CODE = 'ANN' THEN

      SELECT COUNT(*) INTO P_TOTAL_COUNT
        FROM CUST.azbj_imm_annuity_payment
       WHERE proc_status = P_STAGE_CODE;

      OPEN P_RESULT FOR
        SELECT policy_no,
               annuitant_name,       -- TODO: verify column name
               annuity_type,         -- TODO: verify column name
               proc_status           AS stage_code,
               annuity_amount,       -- TODO: verify column name
               payment_date,         -- TODO: verify column name
               frequency,            -- TODO: verify column name
               remarks               -- TODO: verify column name
          FROM CUST.azbj_imm_annuity_payment
         WHERE proc_status = P_STAGE_CODE
         ORDER BY payment_date DESC
         OFFSET V_OFFSET ROWS FETCH NEXT P_PAGE_SIZE ROWS ONLY;

    -- ── Maturity ──────────────────────────────────────────────
    ELSIF P_MODULE_CODE = 'MAT' THEN

      SELECT COUNT(*) INTO P_TOTAL_COUNT
        FROM CUST.azbj_maturity_trad
       WHERE proc_status = P_STAGE_CODE;

      OPEN P_RESULT FOR
        SELECT policy_no,
               policy_holder,        -- TODO: verify column name
               plan_name,            -- TODO: verify column name
               proc_status           AS stage_code,
               maturity_amount,      -- TODO: verify column name
               maturity_date,        -- TODO: verify column name
               sanction_date,        -- TODO: verify column name
               remarks               -- TODO: verify column name
          FROM CUST.azbj_maturity_trad
         WHERE proc_status = P_STAGE_CODE
         ORDER BY maturity_date DESC
         OFFSET V_OFFSET ROWS FETCH NEXT P_PAGE_SIZE ROWS ONLY;

    -- ── Refund ────────────────────────────────────────────────
    ELSIF P_MODULE_CODE = 'REF' THEN

      SELECT COUNT(*) INTO P_TOTAL_COUNT
        FROM CUST.azbj_auto_nb_refund
       WHERE status = P_STAGE_CODE;

      OPEN P_RESULT FOR
        SELECT policy_no,
               proposer_name,        -- TODO: verify column name
               refund_type,          -- TODO: verify column name
               status                AS stage_code,
               refund_amount,        -- TODO: verify column name
               request_date,         -- TODO: verify column name
               processed_date,       -- TODO: verify column name
               remarks               -- TODO: verify column name
          FROM CUST.azbj_auto_nb_refund
         WHERE status = P_STAGE_CODE
         ORDER BY request_date DESC
         OFFSET V_OFFSET ROWS FETCH NEXT P_PAGE_SIZE ROWS ONLY;

    -- ── Surrender ─────────────────────────────────────────────
    ELSIF P_MODULE_CODE = 'SUR' THEN

      SELECT COUNT(*) INTO P_TOTAL_COUNT
        FROM CUST.azbj_surrender
       WHERE event = P_STAGE_CODE;

      OPEN P_RESULT FOR
        SELECT policy_no,
               policy_holder,        -- TODO: verify column name
               surrender_type,       -- TODO: verify column name
               event                 AS stage_code,
               surrender_value,      -- TODO: verify column name
               request_date,         -- TODO: verify column name
               sanction_date,        -- TODO: verify column name
               remarks               -- TODO: verify column name
          FROM CUST.azbj_surrender
         WHERE event = P_STAGE_CODE
         ORDER BY request_date DESC
         OFFSET V_OFFSET ROWS FETCH NEXT P_PAGE_SIZE ROWS ONLY;

    -- ── Loan ──────────────────────────────────────────────────
    ELSIF P_MODULE_CODE = 'LOAN' THEN

      SELECT COUNT(*) INTO P_TOTAL_COUNT
        FROM CUST.azbj_trad_policy_loan
       WHERE proc_status = P_STAGE_CODE;

      OPEN P_RESULT FOR
        SELECT policy_no,
               borrower_name,        -- TODO: verify column name
               loan_type,            -- TODO: verify column name
               proc_status           AS stage_code,
               loan_amount,          -- TODO: verify column name
               loan_date,            -- TODO: verify column name
               repayment_date,       -- TODO: verify column name
               remarks               -- TODO: verify column name
          FROM CUST.azbj_trad_policy_loan
         WHERE proc_status = P_STAGE_CODE
         ORDER BY loan_date DESC
         OFFSET V_OFFSET ROWS FETCH NEXT P_PAGE_SIZE ROWS ONLY;

    ELSE
      -- Unknown module — return empty cursor, do not raise
      OPEN P_RESULT FOR
        SELECT NULL AS policy_no,
               NULL AS customer_name,
               NULL AS product_code,
               NULL AS stage_code,
               NULL AS amount,
               NULL AS request_date,
               NULL AS processed_date,
               NULL AS remarks
          FROM DUAL WHERE 1 = 0;
    END IF;

    LOG_CALL(P_MODULE_CODE, 'GET_STAGE_DRILLDOWN', 'SUCCESS',
             NULL, NULL, ELAPSED_MS(V_START));
  EXCEPTION
    WHEN OTHERS THEN
      LOG_CALL(P_MODULE_CODE, 'GET_STAGE_DRILLDOWN', 'ERROR',
               TO_CHAR(SQLCODE), SQLERRM, 0);
      RAISE;
  END GET_STAGE_DRILLDOWN;

  -- ============================================================
  -- P4: GET_FLEXI_ACCUM_DETAIL
  --   Reads AZBJ_FLEXI_ACCUM_DETAIL for a given policy_ref.
  --   Maps the meaningful columns into the existing
  --   WEO_REC_STRINGS50 object type (50 STRINGVAL slots).
  --
  --   Slot mapping (STRINGVAL1..8 used; rest are NULL):
  --     1  = DUE_DATE            (DD-MON-YYYY)
  --     2  = ACC_INT_AMT         (rounded to 2dp)
  --     3  = CB_GMI_AMOUNT       (rounded)
  --     4  = ACB_AGMI_RATE       (rounded to 4dp)
  --     5  = BENEFIT_TYPE
  --     6  = BENEFIT_FREQ        (human-readable)
  --     7  = TRANSATION_TYPE
  --     8  = ACB_AGMI_AMOUNT     (rounded)
  --     9..50 = NULL (reserved for future use)
  -- ============================================================
  PROCEDURE GET_FLEXI_ACCUM_DETAIL (
    P_POLICY_REF  IN  VARCHAR2,
    P_RESULT      OUT CUST.weo_rec_strings50_list
  ) IS
    V_START TIMESTAMP := SYSTIMESTAMP;
  BEGIN
    SELECT CUST.WEO_REC_STRINGS50(
             TO_CHAR(DUE_DATE, 'DD-MON-YYYY'),          -- SV1: Due Date
             TO_CHAR(ROUND(ACC_INT_AMT, 2)),             -- SV2: Accum Int Amt
             TO_CHAR(ROUND(CB_GMI_AMOUNT)),              -- SV3: CB GMI Amount
             TO_CHAR(ROUND(ACB_AGMI_RATE, 4)),           -- SV4: ACB AGMI Rate
             BENEFIT_TYPE,                               -- SV5: Benefit Type
             CASE                                        -- SV6: Freq (readable)
               WHEN BENEFIT_FREQ = 12 THEN 'MONTHLY'
               WHEN BENEFIT_FREQ = 6  THEN 'HALF-YEARLY'
               WHEN BENEFIT_FREQ = 4  THEN 'QUARTERLY'
               WHEN BENEFIT_FREQ = 1  THEN 'YEARLY'
               ELSE TO_CHAR(BENEFIT_FREQ)
             END,
             TRANSATION_TYPE,                            -- SV7: Transaction Type
             TO_CHAR(ROUND(ACB_AGMI_AMOUNT)),            -- SV8: ACB AGMI Amount
             -- SV9..SV50: Reserved / NULL
             NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
             NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
             NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
             NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
             NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
             NULL, NULL
           )
    BULK COLLECT INTO P_RESULT
      FROM CUST.AZBJ_FLEXI_ACCUM_DETAIL
     WHERE policy_ref = P_POLICY_REF
     ORDER BY DUE_DATE, BENEFIT_TYPE;

    LOG_CALL(P_POLICY_REF, 'GET_FLEXI_ACCUM_DETAIL', 'SUCCESS',
             NULL, NULL, ELAPSED_MS(V_START));
  EXCEPTION
    WHEN OTHERS THEN
      LOG_CALL(P_POLICY_REF, 'GET_FLEXI_ACCUM_DETAIL', 'ERROR',
               TO_CHAR(SQLCODE), SQLERRM, 0);
      RAISE;
  END GET_FLEXI_ACCUM_DETAIL;

  -- ============================================================
  -- P5: GET_AUDIT_LOGS
  --   Returns recent rows from PS_API_CALL_LOG for monitoring.
  --   Filter by module (NULL = all modules).
  --   Results ordered newest-first; limited by P_LIMIT.
  -- ============================================================
  PROCEDURE GET_AUDIT_LOGS (
    P_MODULE_CODE IN  VARCHAR2 DEFAULT NULL,
    P_LIMIT       IN  NUMBER   DEFAULT 100,
    P_RESULT      OUT SYS_REFCURSOR
  ) IS
    V_START TIMESTAMP := SYSTIMESTAMP;
  BEGIN
    OPEN P_RESULT FOR
      SELECT LOG_ID,
             MODULE_CODE,
             PROC_NAME,
             CALL_TS,
             STATUS,
             ERR_CODE,
             ERR_MSG,
             EXEC_MS
        FROM CUST.PS_API_CALL_LOG
       WHERE (P_MODULE_CODE IS NULL
              OR MODULE_CODE = P_MODULE_CODE)
       ORDER BY CALL_TS DESC
       FETCH FIRST P_LIMIT ROWS ONLY;

    LOG_CALL(NULL, 'GET_AUDIT_LOGS', 'SUCCESS',
             NULL, NULL, ELAPSED_MS(V_START));
  EXCEPTION
    WHEN OTHERS THEN
      LOG_CALL(NULL, 'GET_AUDIT_LOGS', 'ERROR',
               TO_CHAR(SQLCODE), SQLERRM, 0);
      RAISE;
  END GET_AUDIT_LOGS;

END PKG_PS_DASHBOARD;
/

-- ═══════════════════════════════════════════════════════════════
-- SMOKE TESTS — Run manually in SQL Developer / SQL*Plus
-- after the package compiles with no errors.
-- ═══════════════════════════════════════════════════════════════

/*
-- 1. Verify package compiled without errors:
SELECT object_name, object_type, status
  FROM all_objects
 WHERE object_name = 'PKG_PS_DASHBOARD'
   AND owner       = 'CUST';
-- Expected: STATUS = VALID for both PACKAGE and PACKAGE BODY

-- 2. Module list
VAR rc REFCURSOR;
EXEC CUST.PKG_PS_DASHBOARD.GET_MODULE_LIST(:rc);
PRINT rc;

-- 3. All stage counts
VAR rc REFCURSOR;
EXEC CUST.PKG_PS_DASHBOARD.GET_STAGE_COUNTS(NULL, :rc);
PRINT rc;

-- 4. SB stage counts only
VAR rc REFCURSOR;
EXEC CUST.PKG_PS_DASHBOARD.GET_STAGE_COUNTS('SB', :rc);
PRINT rc;

-- 5. Maturity drill-down page 1 (replace 'EXTRACT' with actual stage code)
VAR cnt NUMBER;
VAR rc  REFCURSOR;
EXEC CUST.PKG_PS_DASHBOARD.GET_STAGE_DRILLDOWN('MAT','EXTRACT',1,20,:cnt,:rc);
PRINT cnt;
PRINT rc;

-- 6. Flexi accum detail (replace with a real policy_ref from your data)
VAR result CUST.weo_rec_strings50_list;
EXEC CUST.PKG_PS_DASHBOARD.GET_FLEXI_ACCUM_DETAIL('YOUR_POLICY_REF', :result);

-- 7. Audit logs (last 10 entries)
VAR rc REFCURSOR;
EXEC CUST.PKG_PS_DASHBOARD.GET_AUDIT_LOGS(NULL, 10, :rc);
PRINT rc;
*/

-- ─── Grant execute to application user (uncomment when ready) ───
-- GRANT EXECUTE ON CUST.PKG_PS_DASHBOARD TO APP_USER;
