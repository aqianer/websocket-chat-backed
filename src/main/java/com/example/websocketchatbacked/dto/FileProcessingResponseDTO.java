package com.example.websocketchatbacked.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessingResponseDTO {
    
    private boolean success;
    
    private String message;
    
    private Integer chunkCount;
    
    private Long processingTime;
    
    private String taskId;
    
    public static FileProcessingResponseDTO success(Integer chunkCount, Long processingTime) {
        return new FileProcessingResponseDTO(true, "处理成功", chunkCount, processingTime, null);
    }
    
    public static FileProcessingResponseDTO success(String taskId) {
        return new FileProcessingResponseDTO(true, "任务已提交", null, null, taskId);
    }
    
    public static FileProcessingResponseDTO failure(String message) {
        return new FileProcessingResponseDTO(false, message, null, null, null);
    }
}