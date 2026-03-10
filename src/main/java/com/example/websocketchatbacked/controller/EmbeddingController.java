package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.example.websocketchatbacked.dto.*;
import com.example.websocketchatbacked.entity.KbDocument;
import com.example.websocketchatbacked.repository.KbDocumentRepository;
import com.example.websocketchatbacked.service.FileService;
import com.example.websocketchatbacked.service.impl.FileTransactionServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private FileService fileService;

    @Autowired
    private KbDocumentRepository kbDocumentRepository;

    @Autowired
    private FileTransactionServiceImpl fileTransactionServiceImpl;

    /**
     * 可以把预校验接口返回的上传凭证和 SA-Token 的登录态做绑定：预校验通过时，生成一个带用户 ID 和文件哈希的临时 Token，存到 Redis 里并
     * 设置 5 分钟有效期；调用上传接口时，同时传 SA-Token 和这个临时 Token，后端先校验登录态，再校验临时 Token 的有效性和一致性，这样既能防止越权上传，也能确保每个上传请求都经过了预校验。
     * 请在文件上传流程中实现分片哈希计算 + 预校验的完整逻辑：首先使用 spark-md5 对文件进行分片计算并展示进度条，哈希生成后调用后端的预校验接口 /api/file/check；如果返回 ' 文件已存在 ' 则弹窗提示并显示跳转链接，
     * 如果返回 ' 允许上传 ' 及临时凭证 token，则携带 token 调用原上传接口 /api/file/upload 完成上传
     */
    @PostMapping("/file/check/batch")
    @SaCheckLogin
    public ApiResponse<BatchFileCheckResponse> checkFiles(@RequestBody BatchFileCheckRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<FileCheckResultItem> results = fileService.checkFilesService(request, userId);
        return ApiResponse.success(new BatchFileCheckResponse(results));
    }

    @PostMapping("/documents/upload/batch")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ApiResponse<BatchUploadResponse> uploadFiles(@ModelAttribute BatchUploadRequest request) {

        // TODO 批量上传接口 接入AsyncTaskService

        log.info("接收到批量上传请求");
        log.info("files参数：{}", request.getFiles());
        log.info("hashes参数：{}", request.getHashes());
        log.info("tokens参数：{}", request.getTokens());
        log.info("batchConfig参数：{}", request.getBatchConfig());

        Long userId = StpUtil.getLoginIdAsLong();
        log.info("当前用户ID：{}", userId);

        List<UploadResultItem> results = fileService.filesUploadService(request, userId);

        return ApiResponse.success(new BatchUploadResponse(results));
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
            fileTransactionServiceImpl.logOperation(userId, id, "delete", request.getRemoteAddr(), "success", null);

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
                        fileTransactionServiceImpl.logOperation(userId, id, "delete", httpRequest.getRemoteAddr(), "success", null);
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    fileTransactionServiceImpl.logOperation(userId, id, "delete", httpRequest.getRemoteAddr(), "failed", e.getMessage());
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

            fileTransactionServiceImpl.logOperation(userId, id, "download", request.getRemoteAddr(), "success", null);

            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(kbDocument.getFileName())
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
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
            HttpServletRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();

            if (documentId == null || documentId <= 0) {
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

            fileTransactionServiceImpl.logOperation(userId, documentId, "preview", request.getRemoteAddr(), "success", null);

            ContentDisposition disposition = ContentDisposition.inline()
                    .filename(kbDocument.getFileName())
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileSize))
                    .body(resource);

        } catch (NotLoginException | NotPermissionException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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

            ContentDisposition disposition = ContentDisposition.inline()
                    .filename(filename)
                    .build();

            return ResponseEntity.status(206)
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize)
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(416).build();
        }
    }
}
