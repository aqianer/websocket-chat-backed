package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.controller.ws.FileParseEndpoint;
import com.example.websocketchatbacked.dto.BatchConfigDTO;
import com.example.websocketchatbacked.dto.UploadedFileDTO;
import com.example.websocketchatbacked.entity.*;
import com.example.websocketchatbacked.factory.ParserFactory;
import com.example.websocketchatbacked.factory.SplitterFactory;
import com.example.websocketchatbacked.parser.FileParser;
import com.example.websocketchatbacked.repository.FileOperationLogRepository;
import com.example.websocketchatbacked.repository.KbChunkRepository;
import com.example.websocketchatbacked.repository.KbDocumentRepository;
import com.example.websocketchatbacked.repository.es.EsChunkRepository;
import com.example.websocketchatbacked.splitter.FileSplitter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskService.class);

    @Autowired
    private KbDocumentRepository kbDocumentRepository;

    @Autowired
    private FileOperationLogRepository fileOperationLogRepository;

    @Autowired
    private KbChunkRepository kbChunkRepository;

    @Autowired
    private EsChunkRepository esChunkRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ParserFactory parserFactory;

    @Autowired
    private SplitterFactory splitFactory;

    private static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>(Arrays.asList("doc", "docx", "pdf", "txt", "md"));
    private static final long MAX_SINGLE_FILE_SIZE = 100 * 1024 * 1024;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Async("chunkExecutor")
    public void processChunk(Long documentId, String parseStrategy, String segmentStrategy) {
        ProcessResult processResult = new ProcessResult();
        // 初始化返回结果
        processResult.setSuccess(true);
        processResult.setFileId(documentId);

        try {
            // 1. 查询文档信息
            KbDocument document = kbDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("文档ID " + documentId + " 不存在"));

            // 2. 获取对应解析器并解析文档
            FileParser fileParser = parserFactory.getParser(document.getFileType());
            // 解析器返回纯文本内容（适配PDF/Word/Markdown）、流式处理避免OOM
            ParseResult parseResult = fileParser.parse(document.getStoragePath());

            // 3. 分块处理（根据策略分块）
            FileSplitter fileSplitter = splitFactory.getSplitter("heading");
            List<String> splitResult = fileSplitter.split(parseResult);
            log.info("文档{}分块处理开始，共{}行", documentId, splitResult.size());

            // 转换器处理
            List<KbChunk> chunkResult = new ArrayList<>();
            List<EsKbChunk> esKbChunkList = new ArrayList<>();
            for (int i = 0; i < splitResult.size(); i++) {
                KbChunk chunk = new KbChunk();
                chunk.setDocId(document.getId());
                chunk.setContent(splitResult.get(i));
                chunk.setChunkNum(i);
                EsKbChunk esKbChunk = EsKbChunk.from(chunk, document);
                esKbChunkList.add(esKbChunk);
                chunkResult.add(chunk);
            }

            log.info("文档{}分块处理完成，共生成{}个分块", documentId, chunkResult.size());
            // TODO 实现事务支持
            // 4. 持久化存储
            // 4.1 写入MySQL（存储分块元数据）
            kbChunkRepository.saveAll(chunkResult);
            // 4.2 写入ES（用于检索，可转成ES实体）
            esChunkRepository.bulkSave(esKbChunkList);

            // 5. 设置成功结果
            processResult.setChunkResult(chunkResult);
            processResult.setMsg("文档分块处理完成，共生成" + chunkResult.size() + "个分块");

            // 6. 更新文档状态

            // 7. 通过websocket连接发送成功响应给前端
            FileParseEndpoint.sendMessageToUser("", "");


        } catch (IllegalArgumentException e) {
            // 业务异常
            processResult.setSuccess(false);
            processResult.setMsg("业务异常：" + e.getMessage());
            log.error("文档{}分块业务异常", documentId, e);
        } catch (IOException e) {
            // 文件读写异常
            processResult.setSuccess(false);
            processResult.setMsg("文件操作异常：" + e.getMessage());
            log.error("文档{}文件读取异常", documentId, e);
        } catch (Exception e) {
            // 兜底捕获所有异常，避免异步任务崩溃
            processResult.setSuccess(false);
            processResult.setMsg("分块处理异常：" + e.getMessage());
            log.error("文档{}分块处理未知异常", documentId, e);
        }

    }

    /**
     * 异步上传文件方法
     *
     * @param file            要上传的文件（MultipartFile类型）
     * @param userId          上传用户ID
     * @param batchConfigJson 批量配置DTO（包含知识库ID等）
     * @return UploadResult 异步结果，包含上传状态和文件信息
     */
    @Async("uploadExecutor") // 指定自定义线程池
    public UploadResult uploadFile(MultipartFile file, Long userId, String batchConfigJson) {
        // 初始化返回结果
        UploadResult result = new UploadResult();
        String originalFileName = file.getOriginalFilename();
        String storagePath = null;
        Long fileId = null;

        try {
            // 1. 空文件校验
            if (file.isEmpty()) {
                throw new IllegalArgumentException("上传文件为空：" + originalFileName);
            }

            // 2. 文件类型校验
            validateFileType(originalFileName);
            // 3. 文件大小校验
            validateFileSize(file.getSize(), MAX_SINGLE_FILE_SIZE, "单个文件");

            // 4. 保存文件到本地服务器
            storagePath = saveFile(file);
            log.info("文件{}已保存到本地路径：{}", originalFileName, storagePath);

            BatchConfigDTO batchConfig = null;
            if (batchConfigJson != null && !batchConfigJson.isEmpty()) {
                batchConfig = objectMapper.readValue(batchConfigJson, BatchConfigDTO.class);

            }

            // 5. 保存文件记录到数据库
            Long knowledgeBaseId = batchConfig != null ? Long.valueOf(batchConfig.getKnowledgeBaseId()) : null;
            fileId = saveFileRecord(file, userId, storagePath, knowledgeBaseId);
            logOperation(userId, fileId, "upload", null, "success", null);

            // 6. 封装文件DTO（赋值到result，供前端使用）
            Long finalFileId = fileId;
            KbDocument kbDocument = kbDocumentRepository.findById(fileId).orElseThrow(
                    () -> new RuntimeException("文件记录保存后未查询到：fileId=" + finalFileId)
            );
            UploadedFileDTO uploadedFile = new UploadedFileDTO(
                    fileId,
                    originalFileName,
                    file.getSize(),
                    kbDocument.getCreateTime().format(DATE_TIME_FORMATTER),
                    "success"
            );
            // 可选：将DTO存入result（需给UploadResult添加对应字段）
            result.setUploadedFile(uploadedFile);

            // 7. 标记上传成功
            result.setSuccess(true);
            result.setMsg("");

        } catch (Exception e) {
            // 捕获所有异常，标记上传失败
            log.error("文件{}上传失败", originalFileName, e);
            result.setSuccess(false);
            result.setMsg("上传失败：" + e.getMessage());

            // 8. 异常时清理资源：删除文件 + 清理数据库记录
            if (storagePath != null) {
                try {
                    Path filePath = Paths.get(storagePath);
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        log.info("已清理失败文件的本地存储：{}", storagePath);
                    }
                } catch (IOException deleteEx) {
                    log.error("删除失败文件{}的本地存储失败", storagePath, deleteEx);
                    logOperation(userId, null, "cleanup", null, "failed", "删除文件失败: " + deleteEx.getMessage());
                }
            }
            if (fileId != null) {
                try {
                    deleteFileRecord(fileId); // 新增：删除数据库中已插入的记录
                    log.info("已清理失败文件的数据库记录：fileId={}", fileId);
                } catch (Exception deleteDbEx) {
                    log.error("删除失败文件{}的数据库记录失败", fileId, deleteDbEx);
                    logOperation(userId, fileId, "cleanup", null, "failed", "删除数据库记录失败: " + deleteDbEx.getMessage());
                }
            }
            logOperation(userId, fileId, "upload", null, "failed", e.getMessage());
        }

        // 返回异步结果（如果saveFile是耗时操作，建议用supplyAsync包装核心逻辑）
        return result;
    }

    @Async("vectorExecutor")
    public CompletableFuture<Void> generateVector(String chunkId) {
        // 向量化逻辑
        return CompletableFuture.runAsync(() -> {
            // 具体向量化代码
        });
    }

    private void deleteFileRecord(Long fileId) {
        kbDocumentRepository.deleteById(fileId);
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
