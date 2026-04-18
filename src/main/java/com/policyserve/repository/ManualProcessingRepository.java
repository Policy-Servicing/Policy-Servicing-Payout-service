package com.policyserve.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ManualProcessingRepository — uses native Oracle queries to look up
 * POLICY_TRANSACTION records. Stored procedure calls go through EntityManager
 * in ManualProcessingService (not here) per Spring Data JPA 2.2.x best practice.
 */
@Repository
public interface ManualProcessingRepository extends JpaRepository<Object, Long> {

    /**
     * Look up the primary policy record.
     * Returns Object[] rows: [POLICY_NO, REQUEST_ID, MODULE_NAME, CUSTOMER_NAME,
     *                         PRODUCT_ID, CURRENT_STAGE, PAYOUT_AMOUNT,
     *                         DATE_OF_COMMENCEMENT, MATURITY_DATE]
     */
    @Query(value =
        "SELECT PT.POLICY_NO, PT.REQUEST_ID, PT.MODULE_NAME, PT.CUSTOMER_NAME, " +
        "       PT.PRODUCT_ID, PT.CURRENT_STAGE, PT.PAYOUT_AMOUNT, " +
        "       PT.DATE_OF_COMMENCEMENT, PT.MATURITY_DATE " +
        "FROM   POLICY_TRANSACTION PT " +
        "WHERE  PT.POLICY_NO  = :policyNo " +
        "AND    PT.REQUEST_ID = :requestId " +
        "AND    ROWNUM = 1",
        nativeQuery = true)
    List<Object[]> findPolicyTransaction(
        @Param("policyNo")  String policyNo,
        @Param("requestId") String requestId
    );

    /**
     * Return the full stage history for the policy, ordered by entry date.
     * Returns Object[] rows: [STAGE_NAME, STATUS, COMPLETED_AT, ENTRY_DATE]
     */
    @Query(value =
        "SELECT SH.STAGE_NAME, SH.STATUS, SH.COMPLETED_AT, SH.ENTRY_DATE " +
        "FROM   POLICY_STAGE_HISTORY SH " +
        "WHERE  SH.POLICY_NO  = :policyNo " +
        "AND    SH.REQUEST_ID = :requestId " +
        "ORDER  BY SH.ENTRY_DATE ASC NULLS LAST",
        nativeQuery = true)
    List<Object[]> findStageHistory(
        @Param("policyNo")  String policyNo,
        @Param("requestId") String requestId
    );
}
