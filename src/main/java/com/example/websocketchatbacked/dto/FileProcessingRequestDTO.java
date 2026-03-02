package com.example.websocketchatbacked.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessingRequestDTO {
    
    private Long docId;
    
    private Long kbId;
    
    private String filePath;
    
    private String fileExtension;
    
    private String splitStrategy;
    
    private Integer chunkSize;
    
    private Integer overlap;
}