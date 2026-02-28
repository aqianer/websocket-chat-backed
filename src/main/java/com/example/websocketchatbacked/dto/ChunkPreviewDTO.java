package com.example.websocketchatbacked.dto;

public class ChunkPreviewDTO {
    private String content;
    private Integer tokenCount;
    private String vectorStatus;

    public ChunkPreviewDTO() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public String getVectorStatus() {
        return vectorStatus;
    }

    public void setVectorStatus(String vectorStatus) {
        this.vectorStatus = vectorStatus;
    }
}
