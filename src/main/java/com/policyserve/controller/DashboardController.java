package com.policyserve.controller;

import com.policyserve.dto.*;
import com.policyserve.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /** GET /api/v1/dashboard/modules */
    @GetMapping("/modules")
    public ResponseEntity<List<ModuleDto>> getModules() {
        return ResponseEntity.ok(dashboardService.getModules());
    }

    /** GET /api/v1/dashboard/products?module={code} */
    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getProducts(
            @RequestParam(required = false) String module) {
        return ResponseEntity.ok(dashboardService.getProducts(module));
    }

    /** GET /api/v1/dashboard/stage-counts?module=&productId=&dateRange= */
    @GetMapping("/stage-counts")
    public ResponseEntity<StageSummaryDto> getStageCounts(
            @RequestParam(defaultValue = "SB")    String module,
            @RequestParam(required = false)        String productId,
            @RequestParam(defaultValue = "TODAY")  String dateRange) {
        return ResponseEntity.ok(dashboardService.getStageSummary(module, productId, dateRange));
    }

    /** GET /api/v1/dashboard/stage-drill?stage=&module=&productId=&page=&size= */
    @GetMapping("/stage-drill")
    public ResponseEntity<DrillDownResponseDto> getDrillDown(
            @RequestParam          String stage,
            @RequestParam          String module,
            @RequestParam(required = false) String productId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(dashboardService.getDrillDown(stage, module, productId, page, size));
    }

    /** GET /api/v1/dashboard/trend?module=&days= */
    @GetMapping("/trend")
    public ResponseEntity<List<TrendPointDto>> getTrend(
            @RequestParam(defaultValue = "SB") String module,
            @RequestParam(defaultValue = "7")  int    days) {
        return ResponseEntity.ok(dashboardService.getTrend(module, days));
    }
}
