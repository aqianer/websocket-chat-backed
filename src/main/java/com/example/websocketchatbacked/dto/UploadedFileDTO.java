package com.example.websocketchatbacked.dto;

import java.time.LocalDateTime;

public class UploadedFileDTO {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String uploadTime;
    private String status;

    public UploadedFileDTO() {
    }

    public UploadedFileDTO(Long id, String fileName, Long fileSize, String uploadTime, String status) {
        this.id = id;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadTime = uploadTime;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
