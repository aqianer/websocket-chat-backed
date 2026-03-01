package com.example.websocketchatbacked.dto;

import java.util.List;

public class DocumentUploadWizardDTO {
    private Long documentId;
    private String fileName;
    private String status;
    private List<ChunkPreviewDTO> chunkData;
    private String uploadTime;
    private String fileSize;
    private String fileType;

    public DocumentUploadWizardDTO() {
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<ChunkPreviewDTO> getChunkData() {
        return chunkData;
    }

    public void setChunkData(List<ChunkPreviewDTO> chunkData) {
        this.chunkData = chunkData;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
