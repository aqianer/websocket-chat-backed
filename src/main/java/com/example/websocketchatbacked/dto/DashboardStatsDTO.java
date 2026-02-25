package com.example.websocketchatbacked.dto;

import java.math.BigDecimal;

public class DashboardStatsDTO {
    private Long totalUsers;
    private BigDecimal todayRecharge;
    private Long pendingAudit;

    public DashboardStatsDTO() {
    }

    public DashboardStatsDTO(Long totalUsers, BigDecimal todayRecharge, Long pendingAudit) {
        this.totalUsers = totalUsers;
        this.todayRecharge = todayRecharge;
        this.pendingAudit = pendingAudit;
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public BigDecimal getTodayRecharge() {
        return todayRecharge;
    }

    public void setTodayRecharge(BigDecimal todayRecharge) {
        this.todayRecharge = todayRecharge;
    }

    public Long getPendingAudit() {
        return pendingAudit;
    }

    public void setPendingAudit(Long pendingAudit) {
        this.pendingAudit = pendingAudit;
    }
}
