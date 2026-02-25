package com.example.websocketchatbacked.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_number_product", indexes = {
        @Index(name = "uk_number_product",
                columnList = "number_id, product_id, effective_time"),
        @Index(name = "idx_number_id",
                columnList = "number_id"),
        @Index(name = "idx_product_id",
                columnList = "product_id")})
public class UserNumberProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "number_id", nullable = false, length = 32)
    private String numberId;

    @Column(name = "product_id", nullable = false, length = 32)
    private String productId;

    @Column(name = "effective_time", nullable = false)
    private Instant effectiveTime;

    @Column(name = "expire_time", nullable = false)
    private Instant expireTime;

    @Column(name = "status", nullable = false)
    private Byte status;

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

    public String getNumberId() {
        return numberId;
    }

    public void setNumberId(String numberId) {
        this.numberId = numberId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Instant getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(Instant effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public Instant getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Instant expireTime) {
        this.expireTime = expireTime;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
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