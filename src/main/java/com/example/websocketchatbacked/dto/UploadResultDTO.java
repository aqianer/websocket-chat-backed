package com.example.websocketchatbacked.dto;

public class UploadResultDTO {
    private Integer successCount;
    private Integer failCount;

    public UploadResultDTO() {
    }

    public UploadResultDTO(Integer successCount, Integer failCount) {
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }
}
