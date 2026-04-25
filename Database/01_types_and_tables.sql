-- =============================================================
-- FILE: 01_types_and_tables.sql
-- PURPOSE: Types, audit log table, and performance indexes
-- DB: Oracle 19c | Schema: CUST
-- RUN ORDER: 1st
-- =============================================================

-- -------------------------------------------------------
-- SECTION 1: Object Types (existing in DB — skip if present)
-- -------------------------------------------------------
-- NOTE: WEO_REC_STRINGS50 already exists in your schema.
-- Run only if re-creating from scratch.

-- CREATE OR REPLACE TYPE CUST.WEO_REC_STRINGS50 AS OBJECT (
--   STRINGVAL1  VARCHAR2(2000), STRINGVAL2  VARCHAR2(2000),
--   STRINGVAL3  VARCHAR2(2000), STRINGVAL4  VARCHAR2(2000),
--   STRINGVAL5  VARCHAR2(2000), STRINGVAL6  VARCHAR2(2000),
--   STRINGVAL7  VARCHAR2(2000), STRINGVAL8  VARCHAR2(2000),
--   STRINGVAL9  VARCHAR2(2000), STRINGVAL10 VARCHAR2(2000),
--   STRINGVAL11 VARCHAR2(2000), STRINGVAL12 VARCHAR2(2000),
--   STRINGVAL13 VARCHAR2(2000), STRINGVAL14 VARCHAR2(2000),
--   STRINGVAL15 VARCHAR2(2000), STRINGVAL16 VARCHAR2(2000),
--   STRINGVAL17 VARCHAR2(2000), STRINGVAL18 VARCHAR2(2000),
--   STRINGVAL19 VARCHAR2(2000), STRINGVAL20 VARCHAR2(2000),
--   STRINGVAL21 VARCHAR2(2000), STRINGVAL22 VARCHAR2(2000),
--   STRINGVAL23 VARCHAR2(2000), STRINGVAL24 VARCHAR2(2000),
--   STRINGVAL25 VARCHAR2(2000), STRINGVAL26 VARCHAR2(2000),
--   STRINGVAL27 VARCHAR2(2000), STRINGVAL28 VARCHAR2(2000),
--   STRINGVAL29 VARCHAR2(2000), STRINGVAL30 VARCHAR2(2000),
--   STRINGVAL31 VARCHAR2(2000), STRINGVAL32 VARCHAR2(2000),
--   STRINGVAL33 VARCHAR2(2000), STRINGVAL34 VARCHAR2(2000),
--   STRINGVAL35 VARCHAR2(2000), STRINGVAL36 VARCHAR2(2000),
--   STRINGVAL37 VARCHAR2(2000), STRINGVAL38 VARCHAR2(2000),
--   STRINGVAL39 VARCHAR2(2000), STRINGVAL40 VARCHAR2(2000),
--   STRINGVAL41 VARCHAR2(2000), STRINGVAL42 VARCHAR2(2000),
--   STRINGVAL43 VARCHAR2(2000), STRINGVAL44 VARCHAR2(2000),
--   STRINGVAL45 VARCHAR2(2000), STRINGVAL46 VARCHAR2(2000),
--   STRINGVAL47 VARCHAR2(2000), STRINGVAL48 VARCHAR2(2000),
--   STRINGVAL49 VARCHAR2(2000), STRINGVAL50 VARCHAR2(2000)
-- );
-- /
-- CREATE OR REPLACE TYPE CUST.weo_rec_strings50_list AS TABLE OF weo_rec_strings50;
-- /

-- -------------------------------------------------------
-- SECTION 2: API Audit / Call Log Table (NEW)
-- -------------------------------------------------------
CREATE TABLE CUST.PS_API_CALL_LOG (
  LOG_ID      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  MODULE_CODE VARCHAR2(50),
  PROC_NAME   VARCHAR2(100)  NOT NULL,
  CALLED_BY   VARCHAR2(100),
  CALL_TS     TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
  STATUS      VARCHAR2(10)   NOT NULL,   -- SUCCESS | ERROR
  ERR_CODE    VARCHAR2(20),
  ERR_MSG     VARCHAR2(4000),
  EXEC_MS     NUMBER
);

-- Indexes on log table
CREATE INDEX CUST.IDX_PS_LOG_TS     ON CUST.PS_API_CALL_LOG (CALL_TS DESC);
CREATE INDEX CUST.IDX_PS_LOG_MODULE  ON CUST.PS_API_CALL_LOG (MODULE_CODE, CALL_TS DESC);
CREATE INDEX CUST.IDX_PS_LOG_STATUS  ON CUST.PS_API_CALL_LOG (STATUS, CALL_TS DESC);

-- -------------------------------------------------------
-- SECTION 3: Performance Indexes on Source Tables
-- NOTE: Only run if these indexes do not already exist.
-- -------------------------------------------------------

-- Survival Benefit
CREATE INDEX CUST.IDX_SB_OPS_AUTH
  ON CUST.azbj_survival_processing (OPS_auth_flg);

-- Annuity
CREATE INDEX CUST.IDX_ANN_PROC_STATUS
  ON CUST.azbj_imm_annuity_payment (proc_status);

-- Maturity
CREATE INDEX CUST.IDX_MAT_PROC_STATUS
  ON CUST.azbj_maturity_trad (proc_status);

-- Refund
CREATE INDEX CUST.IDX_REF_STATUS
  ON CUST.azbj_auto_nb_refund (status);

-- Surrender
CREATE INDEX CUST.IDX_SUR_EVENT
  ON CUST.azbj_surrender (event);

-- Loan
CREATE INDEX CUST.IDX_LOAN_PROC_STATUS
  ON CUST.azbj_trad_policy_loan (proc_status);

-- Flexi Accum — for policy lookup
CREATE INDEX CUST.IDX_FLEXI_POLICY_REF
  ON CUST.AZBJ_FLEXI_ACCUM_DETAIL (policy_ref, DUE_DATE, BENEFIT_TYPE);

COMMIT;

-- -------------------------------------------------------
-- SECTION 4: Grants for application user (adjust APP_USER)
-- -------------------------------------------------------
-- GRANT SELECT ON CUST.azbj_survival_processing  TO APP_USER;
-- GRANT SELECT ON CUST.azbj_imm_annuity_payment  TO APP_USER;
-- GRANT SELECT ON CUST.azbj_maturity_trad        TO APP_USER;
-- GRANT SELECT ON CUST.azbj_auto_nb_refund       TO APP_USER;
-- GRANT SELECT ON CUST.azbj_surrender            TO APP_USER;
-- GRANT SELECT ON CUST.azbj_trad_policy_loan     TO APP_USER;
-- GRANT SELECT ON CUST.AZBJ_FLEXI_ACCUM_DETAIL   TO APP_USER;
-- GRANT INSERT ON CUST.PS_API_CALL_LOG           TO APP_USER;
