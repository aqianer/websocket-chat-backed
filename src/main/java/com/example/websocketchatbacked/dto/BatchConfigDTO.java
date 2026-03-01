package com.example.websocketchatbacked.dto;

public class BatchConfigDTO {
    private String splitStrategy;
    private String language;

    public BatchConfigDTO() {
    }

    public BatchConfigDTO(String splitStrategy, String language) {
        this.splitStrategy = splitStrategy;
        this.language = language;
    }

    public String getSplitStrategy() {
        return splitStrategy;
    }

    public void setSplitStrategy(String splitStrategy) {
        this.splitStrategy = splitStrategy;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
