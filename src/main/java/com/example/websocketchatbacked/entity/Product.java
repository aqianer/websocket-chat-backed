package com.example.websocketchatbacked.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_product_type",
                columnList = "product_type"),
        @Index(name = "idx_status",
                columnList = "status"),
        @Index(name = "idx_create_time",
                columnList = "create_time")}, uniqueConstraints = {@UniqueConstraint(name = "uk_product_id",
        columnNames = {"product_id"})})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "product_type", nullable = false)
    private Boolean productType;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "effective_mode", nullable = false)
    private Boolean effectiveMode;

    @Column(name = "validity_type", nullable = false)
    private Boolean validityType;

    @Column(name = "status", nullable = false)
    private Boolean status;

    @Column(name = "create_time", nullable = false)
    private Instant createTime;

    @Column(name = "update_time", nullable = false)
    private Instant updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Boolean getProductType() {
        return productType;
    }

    public void setProductType(Boolean productType) {
        this.productType = productType;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Boolean getEffectiveMode() {
        return effectiveMode;
    }

    public void setEffectiveMode(Boolean effectiveMode) {
        this.effectiveMode = effectiveMode;
    }

    public Boolean getValidityType() {
        return validityType;
    }

    public void setValidityType(Boolean validityType) {
        this.validityType = validityType;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }

}