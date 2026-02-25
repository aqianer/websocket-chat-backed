package com.example.websocketchatbacked.service;

import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.dto.RechargeDTO;

public interface RechargeService {

    PageResponse<RechargeDTO> getRechargeList(Integer page, Integer pageSize);

    void auditRecharge(Long id, String status, String remark);
}
