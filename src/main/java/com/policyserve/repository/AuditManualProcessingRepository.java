package com.policyserve.repository;

import com.policyserve.entity.AuditManualProcessing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditManualProcessingRepository extends JpaRepository<AuditManualProcessing, Long> {
}
