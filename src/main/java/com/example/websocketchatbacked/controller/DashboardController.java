package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.websocketchatbacked.dto.ApiResponse;
import com.example.websocketchatbacked.dto.DashboardStatsDTO;
import com.example.websocketchatbacked.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    @SaCheckPermission("user:manage")
    public ApiResponse<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO result = dashboardService.getDashboardStats();
        return ApiResponse.success(result);
    }
}
