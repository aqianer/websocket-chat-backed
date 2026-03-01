package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.dto.BatchConfigDTO;
import com.example.websocketchatbacked.dto.UploadedFileDTO;
import com.example.websocketchatbacked.entity.FileOperationLog;
import com.example.websocketchatbacked.entity.KbDocument;
import com.example.websocketchatbacked.entity.UploadResult;
import com.example.websocketchatbacked.repository.FileOperationLogRepository;
import com.example.websocketchatbacked.repository.KbDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class AsyncTaskService {

    @Autowired
    private KbDocumentRepository kbDocumentRepository;

    @Autowired
    private FileOperationLogRepository fileOperationLogRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>(Arrays.asList("doc", "docx", "pdf", "txt", "md"));
    private static final long MAX_SINGLE_FILE_SIZE = 100 * 1024 * 1024;
    private static final int MAX_FILE_COUNT = 300;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Async("chunkExecutor")
    public CompletableFuture<Void> processChunk(String documentId) {
        // 分块逻辑
        return CompletableFuture.runAsync(() -> {
            // 具体分块代码
        });
    }

    /**
     * 异步上传文件方法
     *
     * @param file 要上传的文件（MultipartFile类型）
     * @return CompletableFuture<UploadResult> 异步结果，包含上传状态和文件信息
     */
    @Async("uploadExecutor")
    public CompletableFuture<UploadResult> uploadFile(MultipartFile file, Long userId, BatchConfigDTO batchConfig) {
        // 创建UploadResult对象，用来记录结果
        UploadResult result = new UploadResult();
        // 设置文件名
        result.setFileName(file.getOriginalFilename());
        try {
            if (!file.isEmpty()) {
                String storagePath = null;
                try {
                    validateFileType(file.getOriginalFilename());
                    validateFileSize(file.getSize(), MAX_SINGLE_FILE_SIZE, "单个文件");

                    storagePath = saveFile(file);
                    Long fileId = saveFileRecord(file, userId, storagePath, batchConfig != null ? Long.valueOf(batchConfig.getKnowledgeBaseId()) : null);
                    logOperation(userId, fileId, "upload", null, "success", null);

                    KbDocument kbDocument = kbDocumentRepository.findById(fileId).orElse(null);
                    if (kbDocument != null) {
                        UploadedFileDTO uploadedFile = new UploadedFileDTO(
                                fileId,
                                file.getOriginalFilename(),
                                file.getSize(),
                                kbDocument.getCreateTime().format(DATE_TIME_FORMATTER),
                                "success"
                        );
                    }
                } catch (Exception e) {
                    if (storagePath != null) {
                        try {
                            Path filePath = Paths.get(storagePath);
                            if (Files.exists(filePath)) {
                                Files.delete(filePath);
                            }
                        } catch (IOException deleteEx) {
                            logOperation(userId, null, "cleanup", null, "failed", "删除文件失败: " + deleteEx.getMessage());
                        }
                    }
                }
            }
            result.setSuccess(true);
        } catch (Exception e) {
            // 如果发生异常，设置success为false，并记录错误信息
            result.setSuccess(false);
            result.setErrorMsg("上传失败：" + e.getMessage());
        }

        // 返回包含结果的CompletableFuture
        return CompletableFuture.completedFuture(result);
    }

    @Async("vectorExecutor")
    public CompletableFuture<Void> generateVector(String chunkId) {
        // 向量化逻辑
        return CompletableFuture.runAsync(() -> {
            // 具体向量化代码
        });
    }


    private void validateFileType(String filename) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String extension = getFileExtension(filename);
        if (!ALLOWED_FILE_TYPES.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型：" + extension);
        }
    }

    private void validateFileSize(long fileSize, long maxSize, String type) {
        if (fileSize > maxSize) {
            throw new IllegalArgumentException(type + "大小超出限制，最大允许：" + (maxSize / 1024 / 1024) + "MB");
        }
    }

    private void logOperation(Long userId, Long fileId, String operationType, String ipAddress, String status, String errorMessage) {
        FileOperationLog log = new FileOperationLog();
        log.setUserId(userId);
        log.setDocId(fileId);
        log.setOperationType(operationType);
        log.setOperationTime(LocalDateTime.now());
        log.setIpAddress(ipAddress);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        fileOperationLogRepository.save(log);
    }

    private String getMimeType(String extension) {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("doc", "application/msword");
        mimeTypes.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        mimeTypes.put("pdf", "application/pdf");
        mimeTypes.put("txt", "text/plain");
        mimeTypes.put("md", "text/markdown");
        return mimeTypes.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    private String saveFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String newFileName = UUID.randomUUID().toString() + "." + extension;

        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        Path filePath = uploadDir.resolve(newFileName);
        file.transferTo(filePath.toFile());

        return filePath.toString();
    }

    private Long saveFileRecord(MultipartFile file, Long userId, String storagePath, Long kbId) {
        KbDocument kbDocument = new KbDocument();
        kbDocument.setKbId(kbId != null ? kbId : 1L);
        kbDocument.setUserId(userId);
        kbDocument.setFileName(file.getOriginalFilename());
        kbDocument.setStoragePath(storagePath);
        kbDocument.setFileSize(file.getSize());
        kbDocument.setFileType(getFileExtension(file.getOriginalFilename()).toUpperCase());
        kbDocument.setChunkCount(0);
        kbDocument.setStatus((byte) 1);
        kbDocument.setCurrentStep((byte) 1);
        kbDocument.setCreateTime(LocalDateTime.now());
        kbDocument.setUpdateTime(LocalDateTime.now());

        KbDocument savedDocument = kbDocumentRepository.save(kbDocument);
        return savedDocument.getId();
    }
}
