package com.example.websocketchatbacked.service;

import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.dto.PackageUsageDTO;

public interface PackageService {

    PageResponse<PackageUsageDTO> getPackageUsageList(Integer page, Integer pageSize);
}
