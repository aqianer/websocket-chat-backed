package com.example.websocketchatbacked.controller;

import com.example.websocketchatbacked.dto.ApiResponse;
import com.example.websocketchatbacked.dto.FileProcessingRequestDTO;
import com.example.websocketchatbacked.dto.FileProcessingResponseDTO;
import com.example.websocketchatbacked.service.FileProcessingCoordinator;
import com.example.websocketchatbacked.splitter.impl.FixedLengthSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/file-processing")
public class FileProcessingController {
    
    @Autowired
    private FileProcessingCoordinator coordinator;
    
    @PostMapping("/process")
    public ApiResponse<FileProcessingResponseDTO> processFile(@RequestBody FileProcessingRequestDTO request) {
        
        log.info("收到文件处理请求: docId={}, kbId={}, strategy={}", 
                request.getDocId(), request.getKbId(), request.getSplitStrategy());
        
        long startTime = System.currentTimeMillis();
        
        try {
            boolean success = coordinator.processFileSync(
                    request.getDocId(),
                    request.getKbId(),
                    request.getFilePath(),
                    request.getFileExtension(),
                    request.getSplitStrategy()
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (success) {
                return ApiResponse.success(FileProcessingResponseDTO.success(null, processingTime));
            } else {
                return ApiResponse.error(500, "文件处理失败");
            }
        } catch (Exception e) {
            log.error("文件处理异常", e);
            return ApiResponse.error(500, "文件处理异常: " + e.getMessage());
        }
    }
    
    @PostMapping("/process-async")
    public ApiResponse<FileProcessingResponseDTO> processFileAsync(@RequestBody FileProcessingRequestDTO request) {
        
        log.info("收到异步文件处理请求: docId={}, kbId={}, strategy={}", 
                request.getDocId(), request.getKbId(), request.getSplitStrategy());
        
        try {
            String taskId = UUID.randomUUID().toString();
            coordinator.processFileAsync(
                    request.getDocId(),
                    request.getKbId(),
                    request.getFilePath(),
                    request.getFileExtension(),
                    request.getSplitStrategy()
            );
            
            return ApiResponse.success(FileProcessingResponseDTO.success(taskId));
        } catch (Exception e) {
            log.error("异步文件处理异常", e);
            return ApiResponse.error(500, "异步文件处理异常: " + e.getMessage());
        }
    }
    
    @PostMapping("/batch-process")
    public ApiResponse<FileProcessingResponseDTO> batchProcessFiles(@RequestBody List<FileProcessingRequestDTO> requests) {
        
        log.info("收到批量文件处理请求: count={}", requests.size());
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<FileProcessingCoordinator.FileProcessingRequest> processingRequests = new ArrayList<>();
            for (FileProcessingRequestDTO dto : requests) {
                processingRequests.add(new FileProcessingCoordinator.FileProcessingRequest(
                        dto.getDocId(),
                        dto.getKbId(),
                        dto.getFilePath(),
                        dto.getFileExtension(),
                        dto.getSplitStrategy()
                ));
            }
            
            boolean success = coordinator.batchProcessFilesSync(processingRequests);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (success) {
                return ApiResponse.success(FileProcessingResponseDTO.success(null, processingTime));
            } else {
                return ApiResponse.error(500, "批量文件处理部分失败");
            }
        } catch (Exception e) {
            log.error("批量文件处理异常", e);
            return ApiResponse.error(500, "批量文件处理异常: " + e.getMessage());
        }
    }
    
    @PostMapping("/process-custom")
    public ApiResponse<FileProcessingResponseDTO> processFileWithCustomSplitter(@RequestBody FileProcessingRequestDTO request) {
        
        log.info("收到自定义分块文件处理请求: docId={}, kbId={}, chunkSize={}, overlap={}", 
                request.getDocId(), request.getKbId(), request.getChunkSize(), request.getOverlap());
        
        long startTime = System.currentTimeMillis();
        
        try {
            int chunkSize = request.getChunkSize() != null ? request.getChunkSize() : 500;
            int overlap = request.getOverlap() != null ? request.getOverlap() : 50;
            
            FixedLengthSplitter splitter = new FixedLengthSplitter(chunkSize, overlap);
            
            boolean success = coordinator.processFileSync(
                    request.getDocId(),
                    request.getKbId(),
                    request.getFilePath(),
                    request.getFileExtension(),
                    request.getSplitStrategy()
            );
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (success) {
                return ApiResponse.success(FileProcessingResponseDTO.success(null, processingTime));
            } else {
                return ApiResponse.error(500, "文件处理失败");
            }
        } catch (Exception e) {
            log.error("自定义分块文件处理异常", e);
            return ApiResponse.error(500, "自定义分块文件处理异常: " + e.getMessage());
        }
    }
}