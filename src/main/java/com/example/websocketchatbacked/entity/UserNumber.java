package com.example.websocketchatbacked.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_number", uniqueConstraints = {@UniqueConstraint(name = "uk_number_id",
        columnNames = {"number_id"})})
public class UserNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "number_id", nullable = false, length = 32)
    private String numberId;

    @Column(name = "number_type", nullable = false)
    private Byte numberType;

    @Column(name = "number_value", nullable = false, length = 20)
    private String numberValue;

    @Column(name = "number_status", nullable = false)
    private Byte numberStatus;

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

    public Byte getNumberType() {
        return numberType;
    }

    public void setNumberType(Byte numberType) {
        this.numberType = numberType;
    }

    public String getNumberValue() {
        return numberValue;
    }

    public void setNumberValue(String numberValue) {
        this.numberValue = numberValue;
    }

    public Byte getNumberStatus() {
        return numberStatus;
    }

    public void setNumberStatus(Byte numberStatus) {
        this.numberStatus = numberStatus;
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