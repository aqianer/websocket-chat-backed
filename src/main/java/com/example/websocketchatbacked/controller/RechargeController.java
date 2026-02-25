package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.websocketchatbacked.dto.ApiResponse;
import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.dto.RechargeAuditRequest;
import com.example.websocketchatbacked.dto.RechargeDTO;
import com.example.websocketchatbacked.service.RechargeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recharge")
public class RechargeController {

    @Autowired
    private RechargeService rechargeService;

    @GetMapping
    @SaCheckPermission("recharge:handle")
    public ApiResponse<PageResponse<RechargeDTO>> getRechargeList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        PageResponse<RechargeDTO> result = rechargeService.getRechargeList(page, pageSize);
        return ApiResponse.success(result);
    }

    @PostMapping("/audit")
    @SaCheckPermission("recharge:audit")
    public ApiResponse<Void> auditRecharge(@Valid @RequestBody RechargeAuditRequest request) {
        rechargeService.auditRecharge(request.getId(), request.getStatus(), request.getRemark());
        return ApiResponse.success(null);
    }
}
