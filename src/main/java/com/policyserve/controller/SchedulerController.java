package com.policyserve.controller;

import com.policyserve.dto.*;
import com.policyserve.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/schedulers")
public class SchedulerController {

    @Autowired
    private SchedulerService schedulerService;

    /** GET /api/v1/schedulers */
    @GetMapping
    public ResponseEntity<List<SchedulerDto>> getAll() {
        return ResponseEntity.ok(schedulerService.getAll());
    }

    /** GET /api/v1/schedulers/{id}/history?limit=20 */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<RunHistoryDto>> getHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(schedulerService.getHistory(id, limit));
    }

    /** POST /api/v1/schedulers/{id}/trigger */
    @PostMapping("/{id}/trigger")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<Map<String, Object>> trigger(@PathVariable String id) {
        schedulerService.trigger(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Scheduler triggered"));
    }

    /** PUT /api/v1/schedulers/{id}/toggle */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchedulerDto> toggle(@PathVariable String id) {
        return ResponseEntity.ok(schedulerService.toggle(id));
    }

    /** PUT /api/v1/schedulers/{id}/cron */
    @PutMapping("/{id}/cron")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchedulerDto> updateCron(
            @PathVariable String id,
            @RequestBody  Map<String, String> body) {
        return ResponseEntity.ok(schedulerService.updateCron(id, body.get("cronExpression")));
    }
}
