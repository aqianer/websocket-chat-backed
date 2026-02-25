package com.example.websocketchatbacked.dto;

import java.time.LocalDateTime;

public class FileListDTO {
    private Long id;
    private String fileName;
    private LocalDateTime uploadTime;
    private LocalDateTime updateTime;
    private Long fileSize;
    private String fileType;

    public FileListDTO() {
    }

    public FileListDTO(Long id, String fileName, LocalDateTime uploadTime, LocalDateTime updateTime, Long fileSize, String fileType) {
        this.id = id;
        this.fileName = fileName;
        this.uploadTime = uploadTime;
        this.updateTime = updateTime;
        this.fileSize = fileSize;
        this.fileType = fileType;
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

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
