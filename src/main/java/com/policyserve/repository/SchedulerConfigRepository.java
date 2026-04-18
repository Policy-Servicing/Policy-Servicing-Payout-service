package com.policyserve.repository;

import com.policyserve.entity.SchedulerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SchedulerConfigRepository extends JpaRepository<SchedulerConfig, String> {
    List<SchedulerConfig> findByModuleCode(String moduleCode);
    List<SchedulerConfig> findByEnabled(Boolean enabled);
    List<SchedulerConfig> findByStatus(String status);
}
