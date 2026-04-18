package com.policyserve.repository;

import com.policyserve.entity.PolicyTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyTransactionRepository extends JpaRepository<PolicyTransaction, String> {

    /** Count records by module and stage */
    long countByModuleCodeAndCurrentStage(String moduleCode, String currentStage);

    /** Count records by module, product and stage */
    long countByModuleCodeAndProductIdAndCurrentStage(String moduleCode, String productId, String currentStage);

    /** Count aging records (agingDays > threshold) by module and stage */
    long countByModuleCodeAndCurrentStageAndAgingDaysGreaterThan(
            String moduleCode, String currentStage, int agingDays);

    /** Get all records for drill-down by module+stage with pagination */
    Page<PolicyTransaction> findByModuleCodeAndCurrentStage(
            String moduleCode, String currentStage, Pageable pageable);

    /** Get all records by module, product and stage */
    Page<PolicyTransaction> findByModuleCodeAndProductIdAndCurrentStage(
            String moduleCode, String productId, String currentStage, Pageable pageable);

    /** Daily counts grouped by stage for trend chart */
    @Query(value = """
        SELECT TRUNC(ENTERED_AT) AS run_date,
               CURRENT_STAGE,
               COUNT(*)          AS cnt
        FROM   POLICY_STAGE
        WHERE  MODULE_CODE = :module
        AND    ENTERED_AT >= SYSDATE - :days
        GROUP BY TRUNC(ENTERED_AT), CURRENT_STAGE
        ORDER BY run_date
        """, nativeQuery = true)
    List<Object[]> findTrendData(@Param("module") String module, @Param("days") int days);
}
