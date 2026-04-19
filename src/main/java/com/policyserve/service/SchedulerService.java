package com.policyserve.service;

import com.policyserve.dto.RunHistoryDto;
import com.policyserve.dto.SchedulerDto;
import com.policyserve.entity.SchedulerConfig;
import com.policyserve.exception.ResourceNotFoundException;
import com.policyserve.repository.SchedulerConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private SchedulerConfigRepository schedulerRepo;

    // ── Get all schedulers ────────────────────────────────────────────────────

    public List<SchedulerDto> getAll() {
        log.info("Fetching all schedulers");
        return schedulerRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ── Run History ───────────────────────────────────────────────────────────
    // No dedicated SCHEDULER_RUN_HISTORY table exists yet, so we generate
    // realistic mock history based on the scheduler's own metadata.
    // This keeps the UI functional during development and integration testing.

    public List<RunHistoryDto> getHistory(String schedulerId, int limit) {
        log.info("Fetching run history for schedulerId={}, limit={}", schedulerId, limit);

        SchedulerConfig scheduler = schedulerRepo.findById(schedulerId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduler not found: " + schedulerId));

        List<RunHistoryDto> history = new ArrayList<>();
        String[] statuses = {"SUCCESS", "SUCCESS", "SUCCESS", "FAILED", "SUCCESS"};
        Random rnd = new Random(schedulerId.hashCode()); // deterministic by scheduler ID

        LocalDateTime base = scheduler.getLastRunTime() != null
                ? scheduler.getLastRunTime()
                : LocalDateTime.now().minusDays(1);

        for (int i = 0; i < Math.min(limit, 10); i++) {
            String status = statuses[i % statuses.length];
            long records  = status.equals("FAILED") ? 0L : (long) (rnd.nextInt(2000) + 50);
            long duration = status.equals("FAILED") ? (long) (rnd.nextInt(500) + 100)
                                                    : (long) (rnd.nextInt(60000) + 1000);

            history.add(RunHistoryDto.builder()
                    .id(UUID.randomUUID().toString())
                    .schedulerId(schedulerId)
                    .runTime(base.minusHours(i * 24L))
                    .status(status)
                    .recordsProcessed(records)
                    .durationMs(duration)
                    .errorMessage(status.equals("FAILED") ? "ORA-01403: No data found in POLICY_STAGE" : null)
                    .triggeredBy(i == 0 ? "MANUAL" : "SCHEDULER")
                    .build());
        }

        return history;
    }

    // ── Trigger ───────────────────────────────────────────────────────────────

    @Transactional
    public void trigger(String schedulerId) {
        log.info("Manually triggering scheduler: {}", schedulerId);
        SchedulerConfig sc = schedulerRepo.findById(schedulerId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduler not found: " + schedulerId));
        sc.setStatus("RUNNING");
        sc.setLastRunTime(LocalDateTime.now());
        schedulerRepo.save(sc);
        // In a real implementation, this would call the Quartz scheduler to fire the job immediately.
        log.info("Scheduler {} set to RUNNING state", schedulerId);
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    @Transactional
    public SchedulerDto toggle(String schedulerId) {
        log.info("Toggling scheduler enabled state: {}", schedulerId);
        SchedulerConfig sc = schedulerRepo.findById(schedulerId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduler not found: " + schedulerId));
        sc.setEnabled(!Boolean.TRUE.equals(sc.getEnabled()));
        return toDto(schedulerRepo.save(sc));
    }

    // ── Update Cron ───────────────────────────────────────────────────────────

    @Transactional
    public SchedulerDto updateCron(String schedulerId, String cronExpression) {
        log.info("Updating cron for scheduler={} to '{}'", schedulerId, cronExpression);
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression must not be blank");
        }
        SchedulerConfig sc = schedulerRepo.findById(schedulerId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduler not found: " + schedulerId));
        sc.setCronExpression(cronExpression.trim());
        return toDto(schedulerRepo.save(sc));
    }

    // ── Mapper ────────────────────────────────────────────────────────────────

    private SchedulerDto toDto(SchedulerConfig sc) {
        return SchedulerDto.builder()
                .id(sc.getSchedulerId())
                .name(sc.getName())
                .jobClass(sc.getJobClass())
                .moduleCode(sc.getModuleCode())
                .cronExpression(sc.getCronExpression())
                .status(sc.getStatus())
                .enabled(sc.getEnabled())
                .lastRunTime(sc.getLastRunTime())
                .lastRunStatus(sc.getLastRunStatus())
                .nextRunTime(sc.getNextRunTime())
                .recordsProcessed(sc.getRecordsProcessed())
                .durationMs(sc.getDurationMs())
                .errorMessage(sc.getErrorMessage())
                .description(sc.getDescription())
                .build();
    }
}
