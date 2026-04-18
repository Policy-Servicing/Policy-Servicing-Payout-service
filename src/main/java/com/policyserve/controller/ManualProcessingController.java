package com.policyserve.controller;

import com.policyserve.ManualProcessingService;
import com.policyserve.dto.PolicyDetailDTO;
import com.policyserve.dto.ProcessRequestDTO;
import com.policyserve.dto.ProcessResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/manual-processing")
public class ManualProcessingController {

    private static final Logger log = LoggerFactory.getLogger(ManualProcessingController.class);

    @Autowired
    private ManualProcessingService service;

    /**
     * GET /api/v1/manual-processing/lookup?policyNo=X&requestId=Y
     * Returns full PolicyDetailDTO including stage history.
     */
    @GetMapping("/lookup")
    public ResponseEntity<PolicyDetailDTO> lookup(
            @RequestParam String policyNo,
            @RequestParam String requestId) {
        log.info("GET /lookup  policyNo={}", policyNo);
        PolicyDetailDTO detail = service.lookupPolicy(policyNo, requestId);
        return ResponseEntity.ok(detail);
    }

    /**
     * POST /api/v1/manual-processing/process
     * Body: ProcessRequestDTO  (policyNo, requestId, action, remarks)
     * Calls Oracle stored procedure and returns ProcessResponseDTO.
     */
    @PostMapping("/process")
    public ResponseEntity<ProcessResponseDTO> process(
            @Valid @RequestBody ProcessRequestDTO request) {
        log.info("POST /process  policyNo={}, action={}", request.getPolicyNo(), request.getAction());
        ProcessResponseDTO response = service.processPolicy(
            request.getPolicyNo(),
            request.getRequestId(),
            request.getAction(),
            request.getRemarks()
        );
        return ResponseEntity.ok(response);
    }
}
