package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.websocketchatbacked.dto.ApiResponse;
import com.example.websocketchatbacked.dto.ConsumptionDTO;
import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.service.ConsumptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/consumption")
public class ConsumptionController {

    @Autowired
    private ConsumptionService consumptionService;

    @GetMapping
    @SaCheckPermission("user:manage")
    public ApiResponse<PageResponse<ConsumptionDTO>> getConsumptionList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        PageResponse<ConsumptionDTO> result = consumptionService.getConsumptionList(page, pageSize);
        return ApiResponse.success(result);
    }
}
