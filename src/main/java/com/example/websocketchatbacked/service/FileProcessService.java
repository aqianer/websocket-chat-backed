//package com.example.websocketchatbacked.service;
//
//import com.example.websocketchatbacked.dto.ChunkPreviewDTO;
//import com.example.websocketchatbacked.dto.FileProcessResponseDTO;
//import com.example.websocketchatbacked.entity.KbChunk;
//import com.example.websocketchatbacked.entity.KbDocument;
//import com.example.websocketchatbacked.repository.KbChunkRepository;
//import com.example.websocketchatbacked.repository.KbDocumentRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//public class FileProcessService {
//
//    @Autowired
//    private KbDocumentRepository kbDocumentRepository;
//
//    @Autowired
//    private KbChunkRepository kbChunkRepository;
//
//    @Transactional
//    public FileProcessResponseDTO processFiles(Long kbId, List<Long> documentIds,
//                                              String parseStrategy, List<String> extractContent,
//                                              String segmentStrategy) {
//
//        log.info("开始批量处理文件: kbId={}, documentIds={}, parseStrategy={}, segmentStrategy={}",
//                kbId, documentIds, parseStrategy, segmentStrategy);
//
//        List<FileProcessResponseDTO.ProcessedDocument> processedDocuments = new ArrayList<>();
//
//        for (Long documentId : documentIds) {
//            try {
//                KbDocument document = kbDocumentRepository.findById(documentId)
//                        .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));
//
//                String filePath = document.getStoragePath();
//                String fileContent = readFileContent(filePath, document.getFileType());
//
//                List<ChunkPreviewDTO> chunkData = segmentContent(fileContent, segmentStrategy);
//
//                saveChunks(documentId, kbId, chunkData);
//
//                FileProcessResponseDTO.ProcessedDocument processedDoc = new FileProcessResponseDTO.ProcessedDocument(
//                        documentId,
//                        document.getFileName(),
//                        fileContent,
//                        chunkData
//                );
//
//                processedDocuments.add(processedDoc);
//
//                log.info("文档处理成功: documentId={}, fileName={}, chunkCount={}",
//                        documentId, document.getFileName(), chunkData.size());
//
//            } catch (Exception e) {
//                log.error("文档处理失败: documentId={}", documentId, e);
//                throw new RuntimeException("文档处理失败: " + documentId + ", " + e.getMessage());
//            }
//        }
//
//        FileProcessResponseDTO response = new FileProcessResponseDTO();
//        response.setProcessedDocuments(processedDocuments);
//
//        return response;
//    }
//
//    private String readFileContent(String filePath, String fileType) throws IOException {
//        File file = new File(filePath);
//        if (!file.exists()) {
//            throw new IOException("文件不存在: " + filePath);
//        }
//
//        String content = Files.readString(Paths.get(filePath));
//
//        log.info("文件读取成功: filePath={}, fileType={}, contentLength={}",
//                filePath, fileType, content.length());
//
//        return content;
//    }
//
//    private List<ChunkPreviewDTO> segmentContent(String content, String segmentStrategy) {
//        List<String> segments = new ArrayList<>();
//
//        switch (segmentStrategy.toLowerCase()) {
//            case "auto":
//                segments = segmentAuto(content);
//                break;
//            case "hierarchy":
//                segments = segmentByHierarchy(content);
//                break;
//            case "custom":
//                segments = segmentCustom(content);
//                break;
//            default:
//                segments = segmentAuto(content);
//        }
//
//        List<ChunkPreviewDTO> chunkData = segments.stream()
//                .map(segment -> {
//                    ChunkPreviewDTO chunk = new ChunkPreviewDTO();
//                    chunk.setContent(segment);
//                    chunk.setTokenCount(segment.length() / 2);
//                    chunk.setVectorStatus("已向量化");
//                    return chunk;
//                })
//                .collect(Collectors.toList());
//
//        log.info("内容分段完成: segmentStrategy={}, segmentCount={}", segmentStrategy, segments.size());
//
//        return chunkData;
//    }
//
//    private List<String> segmentAuto(String content) {
//        List<String> segments = new ArrayList<>();
//        int chunkSize = 500;
//        int overlap = 50;
//
//        for (int i = 0; i < content.length(); i += chunkSize - overlap) {
//            int end = Math.min(i + chunkSize, content.length());
//            String segment = content.substring(i, end).trim();
//            if (!segment.isEmpty()) {
//                segments.add(segment);
//            }
//        }
//
//        return segments;
//    }
//
//    private List<String> segmentByHierarchy(String content) {
//        List<String> segments = new ArrayList<>();
//        String[] lines = content.split("\n");
//        StringBuilder currentSegment = new StringBuilder();
//
//        for (String line : lines) {
//            if (line.trim().startsWith("#")) {
//                if (currentSegment.length() > 0) {
//                    segments.add(currentSegment.toString().trim());
//                    currentSegment = new StringBuilder();
//                }
//            }
//            currentSegment.append(line).append("\n");
//        }
//
//        if (currentSegment.length() > 0) {
//            segments.add(currentSegment.toString().trim());
//        }
//
//        return segments;
//    }
//
//    private List<String> segmentCustom(String content) {
//        List<String> segments = new ArrayList<>();
//        int chunkSize = 800;
//        int overlap = 100;
//
//        for (int i = 0; i < content.length(); i += chunkSize - overlap) {
//            int end = Math.min(i + chunkSize, content.length());
//            String segment = content.substring(i, end).trim();
//            if (!segment.isEmpty()) {
//                segments.add(segment);
//            }
//        }
//
//        return segments;
//    }
//
//    private void saveChunks(Long documentId, Long kbId, List<ChunkPreviewDTO> chunkData) {
//        List<KbChunk> chunks = new ArrayList<>();
//
//        for (int i = 0; i < chunkData.size(); i++) {
//            ChunkPreviewDTO chunkDto = chunkData.get(i);
//            KbChunk chunk = new KbChunk();
//            chunk.setDocId(documentId);
//            chunk.setKbId(kbId);
//            chunk.setContent(chunkDto.getContent());
//            chunk.setChunkNum(i + 1);
//            chunks.add(chunk);
//        }
//
//        kbChunkRepository.saveAll(chunks);
//
//        KbDocument document = kbDocumentRepository.findById(documentId).orElse(null);
//        if (document != null) {
//            document.setChunkCount(chunks.size());
//            kbDocumentRepository.save(document);
//        }
//
//        log.info("分块保存成功: documentId={}, chunkCount={}", documentId, chunks.size());
//    }
//}