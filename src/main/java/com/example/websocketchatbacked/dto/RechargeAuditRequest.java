package com.example.websocketchatbacked.dto;

import jakarta.validation.constraints.NotNull;

public class RechargeAuditRequest {
    @NotNull(message = "充值记录ID不能为空")
    private Long id;

    @NotNull(message = "审核状态不能为空")
    private String status;

    private String remark;

    public RechargeAuditRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
