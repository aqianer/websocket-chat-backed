package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.dto.DashboardStatsDTO;
import com.example.websocketchatbacked.entity.Account;
import com.example.websocketchatbacked.entity.Recharge;
import com.example.websocketchatbacked.repository.AccountRepository;
import com.example.websocketchatbacked.repository.RechargeRepository;
import com.example.websocketchatbacked.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RechargeRepository rechargeRepository;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        long totalUsers = accountRepository.count();

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Recharge> todayRecharges = rechargeRepository.findAll().stream()
                .filter(r -> r.getTime() != null
                        && !r.getTime().isBefore(startOfDay)
                        && r.getTime().isBefore(endOfDay))
                .toList();

        BigDecimal todayRecharge = todayRecharges.stream()
                .map(Recharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingAudit = rechargeRepository.findAll().stream()
                .filter(r -> "待审核".equals(r.getStatus()))
                .count();

        return new DashboardStatsDTO(totalUsers, todayRecharge, pendingAudit);
    }
}
