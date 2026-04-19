package com.policyserve.service;

import com.policyserve.dto.RunHistoryDto;
import com.policyserve.dto.SchedulerDto;
import com.policyserve.entity.SchedulerConfig;
import com.policyserve.exception.ResourceNotFoundException;
import com.policyserve.repository.SchedulerConfigRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SchedulerServiceTest {

    @Mock
    private SchedulerConfigRepository schedulerRepo;

    @InjectMocks
    private SchedulerService schedulerService;

    // ── Helper ─────────────────────────────────────────────────────────────────

    private SchedulerConfig buildScheduler(String id, boolean enabled) {
        SchedulerConfig sc = new SchedulerConfig();
        sc.setSchedulerId(id);
        sc.setName("Test Scheduler " + id);
        sc.setJobClass("com.policyserve.job.TestJob");
        sc.setModuleCode("SB");
        sc.setCronExpression("0 0 6 * * ?");
        sc.setStatus("IDLE");
        sc.setEnabled(enabled);
        sc.setLastRunTime(LocalDateTime.now().minusHours(1));
        sc.setRecordsProcessed(100L);
        sc.setDurationMs(12000L);
        return sc;
    }

    // ── getAll ─────────────────────────────────────────────────────────────────

    @Test
    public void getAll_shouldReturnMappedDtos() {
        SchedulerConfig sc1 = buildScheduler("SCH-001", true);
        SchedulerConfig sc2 = buildScheduler("SCH-002", false);
        when(schedulerRepo.findAll()).thenReturn(Arrays.asList(sc1, sc2));

        List<SchedulerDto> result = schedulerService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("SCH-001");
        assertThat(result.get(0).getEnabled()).isTrue();
        assertThat(result.get(1).getId()).isEqualTo("SCH-002");
        assertThat(result.get(1).getEnabled()).isFalse();
    }

    @Test
    public void getAll_empty_shouldReturnEmptyList() {
        when(schedulerRepo.findAll()).thenReturn(java.util.Collections.emptyList());
        assertThat(schedulerService.getAll()).isEmpty();
    }

    // ── getHistory ─────────────────────────────────────────────────────────────

    @Test
    public void getHistory_shouldReturnUpToLimitEntries() {
        SchedulerConfig sc = buildScheduler("SCH-001", true);
        when(schedulerRepo.findById("SCH-001")).thenReturn(Optional.of(sc));

        List<RunHistoryDto> history = schedulerService.getHistory("SCH-001", 5);

        assertThat(history).hasSize(5);
        assertThat(history).allMatch(h -> "SCH-001".equals(h.getSchedulerId()));
    }

    @Test
    public void getHistory_schedulerNotFound_shouldThrowException() {
        when(schedulerRepo.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulerService.getHistory("INVALID", 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("INVALID");
    }

    @Test
    public void getHistory_shouldContainBothScheduledAndManualTriggers() {
        SchedulerConfig sc = buildScheduler("SCH-002", true);
        when(schedulerRepo.findById("SCH-002")).thenReturn(Optional.of(sc));

        List<RunHistoryDto> history = schedulerService.getHistory("SCH-002", 10);

        long manualCount = history.stream()
                .filter(h -> "MANUAL".equals(h.getTriggeredBy())).count();
        assertThat(manualCount).isGreaterThanOrEqualTo(1); // first entry is always MANUAL
    }

    // ── toggle ────────────────────────────────────────────────────────────────

    @Test
    public void toggle_shouldFlipEnabledState() {
        SchedulerConfig sc = buildScheduler("SCH-003", true);
        when(schedulerRepo.findById("SCH-003")).thenReturn(Optional.of(sc));
        when(schedulerRepo.save(any(SchedulerConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerDto result = schedulerService.toggle("SCH-003");

        assertThat(result.getEnabled()).isFalse(); // was true, should be flipped
    }

    @Test
    public void toggle_disabledScheduler_shouldBecomeEnabled() {
        SchedulerConfig sc = buildScheduler("SCH-004", false);
        when(schedulerRepo.findById("SCH-004")).thenReturn(Optional.of(sc));
        when(schedulerRepo.save(any(SchedulerConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerDto result = schedulerService.toggle("SCH-004");

        assertThat(result.getEnabled()).isTrue();
    }

    @Test
    public void toggle_notFound_shouldThrowException() {
        when(schedulerRepo.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulerService.toggle("GHOST"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── trigger ───────────────────────────────────────────────────────────────

    @Test
    public void trigger_shouldSetStatusToRunning() {
        SchedulerConfig sc = buildScheduler("SCH-005", true);
        when(schedulerRepo.findById("SCH-005")).thenReturn(Optional.of(sc));
        when(schedulerRepo.save(any(SchedulerConfig.class))).thenReturn(sc);

        schedulerService.trigger("SCH-005");

        verify(schedulerRepo, times(1)).save(argThat(s -> "RUNNING".equals(s.getStatus())));
    }

    // ── updateCron ────────────────────────────────────────────────────────────

    @Test
    public void updateCron_shouldPersistNewExpression() {
        SchedulerConfig sc = buildScheduler("SCH-006", true);
        when(schedulerRepo.findById("SCH-006")).thenReturn(Optional.of(sc));
        when(schedulerRepo.save(any(SchedulerConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        SchedulerDto result = schedulerService.updateCron("SCH-006", "0 30 9 * * ?");

        assertThat(result.getCronExpression()).isEqualTo("0 30 9 * * ?");
    }

    @Test
    public void updateCron_blankExpression_shouldThrowIllegalArgument() {
        assertThatThrownBy(() -> schedulerService.updateCron("SCH-007", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cron expression");
    }

    @Test
    public void updateCron_nullExpression_shouldThrowIllegalArgument() {
        assertThatThrownBy(() -> schedulerService.updateCron("SCH-007", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
