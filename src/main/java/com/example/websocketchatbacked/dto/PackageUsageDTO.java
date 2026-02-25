package com.example.websocketchatbacked.dto;

import java.math.BigDecimal;

public class PackageUsageDTO {
    private String username;
    private String phone;
    private String packageName;
    private BigDecimal totalData;
    private BigDecimal usedData;
    private Integer dataPercentage;
    private Integer totalVoice;
    private Integer usedVoice;
    private Integer voicePercentage;
    private Integer totalSms;
    private Integer usedSms;
    private Integer smsPercentage;

    public PackageUsageDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public BigDecimal getTotalData() {
        return totalData;
    }

    public void setTotalData(BigDecimal totalData) {
        this.totalData = totalData;
    }

    public BigDecimal getUsedData() {
        return usedData;
    }

    public void setUsedData(BigDecimal usedData) {
        this.usedData = usedData;
    }

    public Integer getDataPercentage() {
        return dataPercentage;
    }

    public void setDataPercentage(Integer dataPercentage) {
        this.dataPercentage = dataPercentage;
    }

    public Integer getTotalVoice() {
        return totalVoice;
    }

    public void setTotalVoice(Integer totalVoice) {
        this.totalVoice = totalVoice;
    }

    public Integer getUsedVoice() {
        return usedVoice;
    }

    public void setUsedVoice(Integer usedVoice) {
        this.usedVoice = usedVoice;
    }

    public Integer getVoicePercentage() {
        return voicePercentage;
    }

    public void setVoicePercentage(Integer voicePercentage) {
        this.voicePercentage = voicePercentage;
    }

    public Integer getTotalSms() {
        return totalSms;
    }

    public void setTotalSms(Integer totalSms) {
        this.totalSms = totalSms;
    }

    public Integer getUsedSms() {
        return usedSms;
    }

    public void setUsedSms(Integer usedSms) {
        this.usedSms = usedSms;
    }

    public Integer getSmsPercentage() {
        return smsPercentage;
    }

    public void setSmsPercentage(Integer smsPercentage) {
        this.smsPercentage = smsPercentage;
    }
}
