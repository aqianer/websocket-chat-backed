package com.example.websocketchatbacked.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "usage_detail", indexes = {
        @Index(name = "uniq_usage_detail_uniq_usage_detail_record_id",
                columnList = "usage_id"),
        @Index(name = "idx_usage_detail_user_number",
                columnList = "user_number"),
        @Index(name = "idx_usage_detail_rule_id",
                columnList = "rule_id"),
        @Index(name = "idx_usage_detail_product_id",
                columnList = "product_id"),
        @Index(name = "idx_usage_detail_usage_type",
                columnList = "usage_type"),
        @Index(name = "idx_usage_detail_start_time",
                columnList = "usage_start_time")})
public class UsageDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "usage_id", nullable = false, length = 64)
    private String usageId;

    @Column(name = "user_number", nullable = false, length = 20)
    private String userNumber;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "usage_type", nullable = false, length = 20)
    private String usageType;

    @Column(name = "usage_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal usageValue;

    @Column(name = "usage_unit", nullable = false, length = 10)
    private String usageUnit;

    @Column(name = "usage_start_time", nullable = false)
    private Instant usageStartTime;

    @Column(name = "usage_end_time")
    private Instant usageEndTime;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resource_info")
    private String resourceInfo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsageId() {
        return usageId;
    }

    public void setUsageId(String usageId) {
        this.usageId = usageId;
    }

    public String getUserNumber() {
        return userNumber;
    }

    public void setUserNumber(String userNumber) {
        this.userNumber = userNumber;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public BigDecimal getUsageValue() {
        return usageValue;
    }

    public void setUsageValue(BigDecimal usageValue) {
        this.usageValue = usageValue;
    }

    public String getUsageUnit() {
        return usageUnit;
    }

    public void setUsageUnit(String usageUnit) {
        this.usageUnit = usageUnit;
    }

    public Instant getUsageStartTime() {
        return usageStartTime;
    }

    public void setUsageStartTime(Instant usageStartTime) {
        this.usageStartTime = usageStartTime;
    }

    public Instant getUsageEndTime() {
        return usageEndTime;
    }

    public void setUsageEndTime(Instant usageEndTime) {
        this.usageEndTime = usageEndTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getResourceInfo() {
        return resourceInfo;
    }

    public void setResourceInfo(String resourceInfo) {
        this.resourceInfo = resourceInfo;
    }

}