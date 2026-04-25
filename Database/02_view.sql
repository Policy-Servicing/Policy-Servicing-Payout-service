-- =============================================================
-- FILE: 02_view.sql
-- PURPOSE: Unified stage summary view across all 6 modules
-- DB: Oracle 19c | Schema: CUST
-- RUN ORDER: 2nd (after 01_types_and_tables.sql)
-- =============================================================

CREATE OR REPLACE VIEW CUST.V_PS_STAGE_SUMMARY AS
  -- ── Survival Benefit ──────────────────────────────────────
  SELECT 'SB'      AS MODULE_CODE,
         'SB'      AS MODULE_NAME,
         'N'       AS STAGE_CODE,
         'Extract' AS STAGE_LABEL,
         1         AS SORT_ORDER,
         COUNT(*)  AS REC_COUNT
    FROM CUST.azbj_survival_processing
   WHERE OPS_auth_flg = 'N'
  UNION ALL
  SELECT 'SB', 'SB', 'A', 'Approved', 2, COUNT(*)
    FROM CUST.azbj_survival_processing WHERE OPS_auth_flg = 'A'
  UNION ALL
  SELECT 'SB', 'SB', 'E', 'Error', 3, COUNT(*)
    FROM CUST.azbj_survival_processing WHERE OPS_auth_flg = 'E'

  -- ── Annuity ───────────────────────────────────────────────
  UNION ALL
  SELECT 'ANN', 'Annuity', 'EXTRACT', 'Extract', 1, COUNT(*)
    FROM CUST.azbj_imm_annuity_payment WHERE proc_status = 'EXTRACT'
  UNION ALL
  SELECT 'ANN', 'Annuity', 'Pending_Tech', 'Pending Tech', 2, COUNT(*)
    FROM CUST.azbj_imm_annuity_payment WHERE proc_status = 'Pending_Tech'
  UNION ALL
  SELECT 'ANN', 'Annuity', 'PAY', 'Pay', 3, COUNT(*)
    FROM CUST.azbj_imm_annuity_payment WHERE proc_status = 'PAY'
  UNION ALL
  SELECT 'ANN', 'Annuity', 'APPROVE', 'Approve', 4, COUNT(*)
    FROM CUST.azbj_imm_annuity_payment WHERE proc_status = 'APPROVE'

  -- ── Maturity ──────────────────────────────────────────────
  UNION ALL
  SELECT 'MAT', 'Maturity', 'EXTRACT', 'Extract', 1, COUNT(*)
    FROM CUST.azbj_maturity_trad WHERE proc_status = 'EXTRACT'
  UNION ALL
  SELECT 'MAT', 'Maturity', 'Pending_Tech', 'Pending Tech', 2, COUNT(*)
    FROM CUST.azbj_maturity_trad WHERE proc_status = 'Pending_Tech'
  UNION ALL
  SELECT 'MAT', 'Maturity', 'APPROVE', 'Approve', 3, COUNT(*)
    FROM CUST.azbj_maturity_trad WHERE proc_status = 'APPROVE'
  UNION ALL
  SELECT 'MAT', 'Maturity', 'PAID', 'Paid', 4, COUNT(*)
    FROM CUST.azbj_maturity_trad WHERE proc_status = 'PAID'
  UNION ALL
  SELECT 'MAT', 'Maturity', 'SANCTION', 'Sanction', 5, COUNT(*)
    FROM CUST.azbj_maturity_trad WHERE proc_status = 'SANCTION'

  -- ── Refund ────────────────────────────────────────────────
  UNION ALL
  SELECT 'REF', 'Refund', 'R', 'Extract', 1, COUNT(*)
    FROM CUST.azbj_auto_nb_refund WHERE status = 'R'
  UNION ALL
  SELECT 'REF', 'Refund', 'P', 'Paid', 2, COUNT(*)
    FROM CUST.azbj_auto_nb_refund WHERE status = 'P'

  -- ── Surrender ─────────────────────────────────────────────
  UNION ALL
  SELECT 'SUR', 'Surrender', 'REQUEST', 'Request', 1, COUNT(*)
    FROM CUST.azbj_surrender WHERE event = 'REQUEST'
  UNION ALL
  SELECT 'SUR', 'Surrender', 'APPROVED', 'Approved', 2, COUNT(*)
    FROM CUST.azbj_surrender WHERE event = 'APPROVED'
  UNION ALL
  SELECT 'SUR', 'Surrender', 'SANCTION', 'Sanction', 3, COUNT(*)
    FROM CUST.azbj_surrender WHERE event = 'SANCTION'

  -- ── Loan ──────────────────────────────────────────────────
  UNION ALL
  SELECT 'LOAN', 'Loan', 'Quotation', 'Quotation', 1, COUNT(*)
    FROM CUST.azbj_trad_policy_loan WHERE proc_status = 'Quotation'
  UNION ALL
  SELECT 'LOAN', 'Loan', 'Paid', 'Paid', 2, COUNT(*)
    FROM CUST.azbj_trad_policy_loan WHERE proc_status = 'Paid'
  UNION ALL
  SELECT 'LOAN', 'Loan', 'Pending_L1', 'Pending L1', 3, COUNT(*)
    FROM CUST.azbj_trad_policy_loan WHERE proc_status = 'Pending_L1'
  UNION ALL
  SELECT 'LOAN', 'Loan', 'Pending_L2', 'Pending L2', 4, COUNT(*)
    FROM CUST.azbj_trad_policy_loan WHERE proc_status = 'Pending_L2';

-- Quick smoke test (run manually after creation):
-- SELECT * FROM CUST.V_PS_STAGE_SUMMARY ORDER BY MODULE_CODE, SORT_ORDER;
