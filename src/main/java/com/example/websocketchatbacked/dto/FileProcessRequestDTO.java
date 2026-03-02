package com.example.websocketchatbacked.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessRequestDTO {
    
    private Long kbId;
    
    private List<Long> documentIds;
    
    private String parseStrategy;
    
    private List<String> extractContent;
    
    private String segmentStrategy;
}