package com.example.websocketchatbacked.dto;

public class FileCheckResponse {
    private Boolean exists;
    private String message;
    private Long documentId;
    private String token;

    public FileCheckResponse() {
    }

    public FileCheckResponse(Boolean exists, String message) {
        this.exists = exists;
        this.message = message;
    }

    public FileCheckResponse(Boolean exists, String message, Long documentId, String token) {
        this.exists = exists;
        this.message = message;
        this.documentId = documentId;
        this.token = token;
    }

    public static FileCheckResponse exists(Long documentId) {
        return new FileCheckResponse(true, "文件已存在", documentId, null);
    }

    public static FileCheckResponse notExists(String token) {
        return new FileCheckResponse(false, "允许上传", null, token);
    }

    public Boolean getExists() {
        return exists;
    }

    public void setExists(Boolean exists) {
        this.exists = exists;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
