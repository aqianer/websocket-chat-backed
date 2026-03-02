package com.example.websocketchatbacked.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessResponseDTO {
    
    private List<ProcessedDocument> processedDocuments;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessedDocument {
        private Long documentId;
        private String fileName;
        private String originalContent;
        private List<ChunkPreviewDTO> chunkData;
    }
}