package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.dto.PageResponse;
import com.example.websocketchatbacked.dto.RechargeDTO;
import com.example.websocketchatbacked.entity.Recharge;
import com.example.websocketchatbacked.repository.RechargeRepository;
import com.example.websocketchatbacked.service.RechargeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RechargeServiceImpl implements RechargeService {

    @Autowired
    private RechargeRepository rechargeRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public PageResponse<RechargeDTO> getRechargeList(Integer page, Integer pageSize) {
        int pageNum = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum - 1, size, Sort.by(Sort.Direction.DESC, "time"));
        Page<Recharge> rechargePage = rechargeRepository.findAll(pageable);

        List<RechargeDTO> dtoList = rechargePage.getContent().stream().map(recharge -> {
            RechargeDTO dto = new RechargeDTO();
            dto.setId(recharge.getId());
            dto.setPhone(recharge.getPhone());
            dto.setAmount(recharge.getAmount());
            dto.setTime(recharge.getTime() != null ? DATE_FORMATTER.format(recharge.getTime()) : "");
            dto.setStatus(recharge.getStatus());
            dto.setMethod(recharge.getMethod());
            return dto;
        }).collect(Collectors.toList());

        return new PageResponse<>(dtoList, rechargePage.getTotalElements());
    }

    @Override
    public void auditRecharge(Long id, String status, String remark) {
        Recharge recharge = rechargeRepository.findById(id).orElse(null);
        if (recharge == null) {
            throw new RuntimeException("充值记录不存在");
        }

        if ("approved".equals(status)) {
            recharge.setStatus("已审核");
        } else if ("rejected".equals(status)) {
            recharge.setStatus("已拒绝");
        } else {
            throw new RuntimeException("无效的审核状态");
        }

        if (remark != null) {
            recharge.setRemark(remark);
        }

        rechargeRepository.save(recharge);
    }
}
