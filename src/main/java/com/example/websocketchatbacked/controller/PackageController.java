package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.websocketchatbacked.dto.ApiResponse;
import com.example.websocketchatbacked.dto.PackageUsageDTO;
import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.service.PackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/package")
public class PackageController {

    @Autowired
    private PackageService packageService;

    @GetMapping
    @SaCheckPermission("user:manage")
    public ApiResponse<PageResponse<PackageUsageDTO>> getPackageUsageList(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        PageResponse<PackageUsageDTO> result = packageService.getPackageUsageList(page, pageSize);
        return ApiResponse.success(result);
    }
}
