package com.example.websocketchatbacked.dto;

import java.util.List;

public class BatchUploadResponseDTO {
    private Integer successCount;
    private Integer failCount;
    private List<UploadedFileDTO> uploadedFiles;
    private List<UploadErrorDTO> errors;

    public BatchUploadResponseDTO() {
    }

    public BatchUploadResponseDTO(Integer successCount, Integer failCount, List<UploadedFileDTO> uploadedFiles, List<UploadErrorDTO> errors) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.uploadedFiles = uploadedFiles;
        this.errors = errors;
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

    public List<UploadedFileDTO> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<UploadedFileDTO> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public List<UploadErrorDTO> getErrors() {
        return errors;
    }

    public void setErrors(List<UploadErrorDTO> errors) {
        this.errors = errors;
    }
}
