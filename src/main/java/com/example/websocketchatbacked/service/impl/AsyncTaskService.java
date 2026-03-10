package com.example.websocketchatbacked.service.impl;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.example.websocketchatbacked.controller.ws.FileParseEndpoint;
import com.example.websocketchatbacked.dto.UploadResultItem;
import com.example.websocketchatbacked.entity.EsKbChunk;
import com.example.websocketchatbacked.entity.KbChunk;
import com.example.websocketchatbacked.entity.KbDocument;
import com.example.websocketchatbacked.entity.ProcessResult;
import com.example.websocketchatbacked.factory.ParserFactory;
import com.example.websocketchatbacked.factory.SplitterFactory;
import com.example.websocketchatbacked.parser.result.FileParser;
import com.example.websocketchatbacked.parser.result.ParseResult;
import com.example.websocketchatbacked.repository.FileOperationLogRepository;
import com.example.websocketchatbacked.repository.KbChunkRepository;
import com.example.websocketchatbacked.repository.KbDocumentRepository;
import com.example.websocketchatbacked.repository.es.EsChunkRepository;
import com.example.websocketchatbacked.service.FileTransactionService;
import com.example.websocketchatbacked.splitter.FileSplitter;
import com.example.websocketchatbacked.util.SimHashUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AsyncTaskService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskService.class);

    @Autowired
    private FileTransactionService fileTransactionService;

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

    @Autowired
    private VectorStore vectorStore;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Async("chunkExecutor")
    public void processChunk(Long documentId, String parseStrategy, String segmentStrategy, Long kbId) {
        ProcessResult processResult = new ProcessResult();
        // 初始化返回结果
        processResult.setSuccess(true);
        processResult.setFileId(documentId);

        try {
            // 1. 查询文档信息
            KbDocument document = kbDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("文档ID " + documentId + " 不存在"));

            // 2. 获取对应解析器并解析文档
            FileParser fileParser = parserFactory.getParser(document.getFileType().toLowerCase());
            // 解析器返回纯文本内容（适配PDF/Word/Markdown）、流式处理避免OOM
            ParseResult parseResult = fileParser.parse(document.getStoragePath());
            log.info("文档{}解析完成，parseResult为{}", documentId, parseResult);

            // 3. TODO 分块处理（根据策略分块）
            FileSplitter fileSplitter = splitFactory.getSplitter("heading");
            List<String> splitResult = fileSplitter.split(parseResult);
            log.info("文档{}分块处理开始，共{}行", documentId, splitResult.size());

            // 使用 Stream 流进行转换器处理
            List<KbChunk> chunkResult = splitResult.stream()
                    .map(content -> {
                        KbChunk chunk = new KbChunk();
                        chunk.setDocId(document.getId());
                        chunk.setKbId(kbId);
                        chunk.setContent(content);
                        chunk.setChunkNum(splitResult.indexOf(content));
                        chunk.setSimHash(SimHashUtil.getSimHash(content));
                        return chunk;
                    })
                    .collect(Collectors.toList());

            log.info("文档{}分块处理完成，共生成{}个分块", documentId, chunkResult.size());
            // TODO 实现事务支持

            // TODO 更新表结构以后会多出计算sim_hash和比较相似分块的过程，相似的分块值存首次保存的分块内容 ，后续相似分块直接用一个master_chunk_id 字段去指向，都需要记录 metadata
            // 4. 持久化存储
            // 写入MySQL（存储分块元数据）
            List<KbChunk> kbChunks = kbChunkRepository.saveAll(chunkResult);
            List<EsKbChunk> esChunks = kbChunks.stream().map(chunk -> EsKbChunk.from(chunk, document)).collect(Collectors.toList());

            // 写入ES（用于检索，可转成ES实体）
            esChunkRepository.bulkSave(esChunks);

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
     * @param file
     * @param userId
     * @param knowledgeBaseId
     * @return
     */
    @Async("uploadExecutor")
    public CompletableFuture<UploadResultItem> uploadFile(MultipartFile file, Long userId, Long knowledgeBaseId, String hash, String fileType) {
        // 初始化返回结果
        UploadResultItem result = new UploadResultItem();
        String originalFileName = file.getOriginalFilename();
        String storagePath = null;
        Long fileId = null;

        try {
            // 保存文件到本地服务器
            storagePath = saveFile(file);
            log.info("文件{}已保存到本地路径：{}", originalFileName, storagePath);

            // 调用事务方法
            fileId = fileTransactionService.saveFileRecord(file, userId, storagePath, knowledgeBaseId, hash, fileType);
            log.info("文件{}已保存到数据库，ID为：{}", originalFileName, fileId);
            result.setDocumentId(fileId);

            // 标记上传成功
            result.setSuccess(true);
            result.setMessage("success");

        } catch (Exception e) {
            // 捕获所有异常，标记上传失败
            log.error("文件{}上传失败", originalFileName, e);
            result.setSuccess(false);
            result.setMessage("上传失败：" + e.getMessage());

            // 异常时清理资源：删除文件
            if (storagePath != null) {
                try {
                    Path filePath = Paths.get(storagePath);
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        log.info("已清理失败文件的本地存储：{}", storagePath);
                    }
                } catch (IOException deleteEx) {
                    log.error("删除失败文件{}的本地存储失败", storagePath, deleteEx);
                    fileTransactionService.logOperation(userId, null, "cleanup", null, "failed", "删除文件失败: " + deleteEx.getMessage());
                }
            }

            fileTransactionService.logOperation(userId, fileId, "upload", null, "failed", e.getMessage());
        }

        // 返回异步结果（如果saveFile是耗时操作，建议用supplyAsync包装核心逻辑）
        return CompletableFuture.completedFuture(result);
    }

    @Async("vectorExecutor")
    public void generateVectorsForDocuments(Long documentId) {
        CompletableFuture.runAsync(() -> {
            log.info("开始向量化文档，文档ID：{}", documentId);

            try {
                List<KbChunk> chunks = kbChunkRepository.findByDocIdOrderByChunkNum(documentId);
                log.info("文档ID {} 找到 {} 个分块", documentId, chunks.size());

                // 注意判空,如果分块内容是空的就代表的是已经向量化
                List<Document> documents = chunks.stream().filter(chunk -> chunk.getContent() != null && !chunk.getContent().isEmpty()).map(chunk -> new Document(chunk.getId().toString(), chunk.getContent(), new HashMap<>())).toList();
                log.info("向量化文档 {} 中有 {} 个非空的分块", documentId, documents.size());
                vectorStore.add(documents);
                log.info("文档ID {} 所有分块向量化完成", documentId);
            } catch (Exception e) {
                log.error("文档ID {} 向量化失败", documentId, e);
            }
        });
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


}
