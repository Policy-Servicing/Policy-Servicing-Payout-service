package com.policyserve.controller;

import com.policyserve.AuditService;
import com.policyserve.dto.AuditLogDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    @Autowired
    private AuditService auditService;

    /**
     * POST /api/v1/audit/log
     * Called by the Angular frontend after each successful manual processing action.
     */
    @PostMapping("/log")
    public ResponseEntity<Void> logAction(@Valid @RequestBody AuditLogDTO dto) {
        log.info("Received audit log request: policyNo={}, action={}", dto.getPolicyNo(), dto.getActionTaken());
        auditService.logAction(dto);
        return ResponseEntity.accepted().build();
    }
}
