package com.example.websocketchatbacked.dto;

public class UploadErrorDTO {
    private String fileName;
    private String error;

    public UploadErrorDTO() {
    }

    public UploadErrorDTO(String fileName, String error) {
        this.fileName = fileName;
        this.error = error;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
