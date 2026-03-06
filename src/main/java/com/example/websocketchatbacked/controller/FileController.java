package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.example.websocketchatbacked.dto.*;
import com.example.websocketchatbacked.entity.FileOperationLog;
import com.example.websocketchatbacked.entity.KbDocument;
import com.example.websocketchatbacked.entity.ProcessResult;
import com.example.websocketchatbacked.repository.FileOperationLogRepository;
import com.example.websocketchatbacked.repository.KbDocumentRepository;
import com.example.websocketchatbacked.service.impl.AsyncTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class FileController {

    private static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>(Arrays.asList("doc", "docx", "pdf", "txt", "md"));
    private static final long MAX_SINGLE_FILE_SIZE = 100 * 1024 * 1024;
    private static final int MAX_FILE_COUNT = 300;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private KbDocumentRepository kbDocumentRepository;

    @Autowired
    private FileOperationLogRepository fileOperationLogRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsyncTaskService asyncTaskService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String FILE_UPLOAD_TOKEN_PREFIX = "file:upload:token:";


    //TODO 可以把预校验接口返回的上传凭证和 SA-Token 的登录态做绑定：预校验通过时，生成一个带用户 ID 和文件哈希的临时 Token，存到 Redis 里并
    // 设置 5 分钟有效期；调用上传接口时，同时传 SA-Token 和这个临时 Token，后端先校验登录态，再校验临时 Token 的有效性和一致性，这样既能防止越权上传，也能确保每个上传请求都经过了预校验。
    // 请在文件上传流程中实现分片哈希计算 + 预校验的完整逻辑：首先使用 spark-md5 对文件进行分片计算并展示进度条，哈希生成后调用后端的预校验接口 /api/file/check；如果返回 ' 文件已存在 ' 则弹窗提示并显示跳转链接，
    // 如果返回 ' 允许上传 ' 及临时凭证 token，则携带 token 调用原上传接口 /api/file/upload 完成上传
    @PostMapping("/file/check")
    @SaCheckLogin
    public ApiResponse<FileCheckResponse> checkFile(@RequestBody FileCheckRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (request.getFileHash() == null || request.getFileHash().isEmpty()) {
                return ApiResponse.error(400, "文件哈希值不能为空");
            }

            Optional<KbDocument> existingDocument = kbDocumentRepository.findByFileHash(request.getFileHash());
            if (existingDocument.isPresent()) {
                KbDocument doc = existingDocument.get();
                return ApiResponse.success(FileCheckResponse.exists(doc.getId()));
            }

            String token = UUID.randomUUID().toString();
            String tokenKey = FILE_UPLOAD_TOKEN_PREFIX + token;
            String tokenValue = userId + ":" + request.getFileHash();
            stringRedisTemplate.opsForValue().set(tokenKey, tokenValue, 5, TimeUnit.MINUTES);

            return ApiResponse.success(FileCheckResponse.notExists(token));

        } catch (NotLoginException e) {
            return ApiResponse.error(401, "未授权，请先登录");
        } catch (Exception e) {
            return ApiResponse.error(500, "文件校验失败：" + e.getMessage());
        }
    }

    @PostMapping("/documents/upload")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ApiResponse<BatchUploadResponseDTO> uploadFile(
            @RequestParam(value = "files[]", required = false) List<MultipartFile> files,
            @RequestParam(value = "batchConfig", required = false) String batchConfigJson) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (files == null || files.isEmpty()) {
                return ApiResponse.error(400, "请选择要上传的文件");
            }

            if (files.size() > MAX_FILE_COUNT) {
                return ApiResponse.error(400, "文件数量超出限制，最多支持 " + MAX_FILE_COUNT + " 个文件");
            }

            BatchConfigDTO batchConfig = null;
            if (batchConfigJson != null && !batchConfigJson.isEmpty()) {
                try {
                    batchConfig = objectMapper.readValue(batchConfigJson, BatchConfigDTO.class);
                } catch (Exception e) {
                    return ApiResponse.error(400, "batchConfig 格式错误");
                }
            }

            List<UploadedFileDTO> uploadedFiles = new ArrayList<>();
            List<UploadErrorDTO> errors = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (MultipartFile file : files) {
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
                            uploadedFiles.add(uploadedFile);
                            successCount++;
                        }
                    } catch (Exception e) {
                        failCount++;
                        errors.add(new UploadErrorDTO(file.getOriginalFilename(), e.getMessage()));

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
            }

            BatchUploadResponseDTO response = new BatchUploadResponseDTO(successCount, failCount, uploadedFiles, errors);
            return new ApiResponse<>(200, "上传成功", response);

        } catch (NotLoginException e) {
            return ApiResponse.error(401, "未授权，请先登录");
        } catch (NotPermissionException e) {
            return ApiResponse.error(403, "权限不足，仅超级管理员可操作");
        } catch (MaxUploadSizeExceededException e) {
            return ApiResponse.error(400, "文件大小超出限制");
        } catch (MultipartException e) {
            return ApiResponse.error(400, "文件上传失败：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/file/list")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ApiResponse<List<FileListDTO>> getFileList() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            List<KbDocument> kbDocuments = kbDocumentRepository.findAll();

            List<FileListDTO> fileList = kbDocuments.stream()
                    .map(doc -> new FileListDTO(
                            doc.getId(),
                            doc.getFileName(),
                            doc.getCreateTime(),
                            doc.getUpdateTime(),
                            doc.getFileSize(),
                            doc.getFileType()
                    ))
                    .collect(Collectors.toList());

            return ApiResponse.success(fileList);

        } catch (NotLoginException e) {
            return ApiResponse.error(401, "用户未登录");
        } catch (NotPermissionException e) {
            return ApiResponse.error(403, "权限不足，仅超级管理员可操作");
        } catch (Exception e) {
            return ApiResponse.error(500, "获取文件列表失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/file/{id}")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ApiResponse<Void> deleteFile(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Optional<KbDocument> kbDocumentOpt = kbDocumentRepository.findById(id);

            if (kbDocumentOpt.isEmpty()) {
                return ApiResponse.error(404, "文件不存在");
            }

            KbDocument kbDocument = kbDocumentOpt.get();
            Path filePath = Paths.get(kbDocument.getStoragePath());

            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            kbDocumentRepository.deleteById(id);
            logOperation(userId, id, "delete", request.getRemoteAddr(), "success", null);

            return ApiResponse.success(null);

        } catch (NotLoginException e) {
            return ApiResponse.error(401, "用户未登录");
        } catch (NotPermissionException e) {
            return ApiResponse.error(403, "权限不足，仅超级管理员可操作");
        } catch (IOException e) {
            return ApiResponse.error(500, "删除文件失败：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "删除失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/file/batch")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ApiResponse<Map<String, Integer>> batchDeleteFiles(@RequestBody BatchDeleteRequest request, HttpServletRequest httpRequest) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            List<Long> ids = request.getIds();

            if (ids == null || ids.isEmpty()) {
                return ApiResponse.error(400, "请选择要删除的文件");
            }

            int successCount = 0;
            int failCount = 0;

            for (Long id : ids) {
                try {
                    Optional<KbDocument> kbDocumentOpt = kbDocumentRepository.findById(id);
                    if (kbDocumentOpt.isPresent()) {
                        KbDocument kbDocument = kbDocumentOpt.get();
                        Path filePath = Paths.get(kbDocument.getStoragePath());

                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        }

                        kbDocumentRepository.deleteById(id);
                        logOperation(userId, id, "delete", httpRequest.getRemoteAddr(), "success", null);
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    logOperation(userId, id, "delete", httpRequest.getRemoteAddr(), "failed", e.getMessage());
                }
            }

            Map<String, Integer> data = new HashMap<>();
            data.put("successCount", successCount);
            data.put("failCount", failCount);
            return ApiResponse.success(data);

        } catch (NotLoginException e) {
            return ApiResponse.error(401, "用户未登录");
        } catch (NotPermissionException e) {
            return ApiResponse.error(403, "权限不足，仅超级管理员可操作");
        } catch (Exception e) {
            return ApiResponse.error(500, "批量删除失败：" + e.getMessage());
        }
    }

    @GetMapping("/file/download/{id}")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Optional<KbDocument> kbDocumentOpt = kbDocumentRepository.findById(id);

            if (kbDocumentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            KbDocument kbDocument = kbDocumentOpt.get();
            Path filePath = Paths.get(kbDocument.getStoragePath());

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());
            String contentType = getMimeType(kbDocument.getFileType());

            logOperation(userId, id, "download", request.getRemoteAddr(), "success", null);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + kbDocument.getFileName() + "\"")
                    .body(resource);

        } catch (NotLoginException | NotPermissionException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/documents/{documentId}/preview")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ResponseEntity<Resource> previewDocument(
            @PathVariable Long documentId,
            @RequestParam String verifyCode,
            HttpServletRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (documentId == null || documentId <= 0) {
                return ResponseEntity.badRequest().build();
            }

            if (verifyCode == null || verifyCode.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Optional<KbDocument> kbDocumentOpt = kbDocumentRepository.findById(documentId);
            if (kbDocumentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            KbDocument kbDocument = kbDocumentOpt.get();
            Path filePath = Paths.get(kbDocument.getStoragePath());

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            long fileSize = Files.size(filePath);
            Resource resource = new FileSystemResource(filePath.toFile());
            String contentType = getMimeType(kbDocument.getFileType());

            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                return handleRangeRequest(resource, rangeHeader, fileSize, contentType, kbDocument.getFileName());
            }

            logOperation(userId, documentId, "preview", request.getRemoteAddr(), "success", null);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + kbDocument.getFileName() + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .body(resource);

        } catch (NotLoginException | NotPermissionException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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

    private ResponseEntity<Resource> handleRangeRequest(Resource resource, String rangeHeader, long fileSize, String contentType, String filename) {
        try {
            String[] ranges = rangeHeader.substring(6).split("-");
            long start = Long.parseLong(ranges[0]);
            long end = ranges.length > 1 ? Long.parseLong(ranges[1]) : fileSize - 1;

            if (start >= fileSize || end >= fileSize || start > end) {
                return ResponseEntity.status(416)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }

            long contentLength = end - start + 1;

            return ResponseEntity.status(206)
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(416).build();
        }
    }
}
