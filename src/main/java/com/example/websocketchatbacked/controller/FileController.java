package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.example.websocketchatbacked.dto.ApiResponse;
import com.example.websocketchatbacked.dto.BatchDeleteRequest;
import com.example.websocketchatbacked.dto.FileListDTO;
import com.example.websocketchatbacked.entity.FileOperationLog;
import com.example.websocketchatbacked.entity.FileRecord;
import com.example.websocketchatbacked.repository.FileOperationLogRepository;
import com.example.websocketchatbacked.repository.FileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileOperationLogRepository fileOperationLogRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    private static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>(Arrays.asList("doc", "docx", "pdf", "txt", "jpg", "png"));
    private static final long MAX_SINGLE_FILE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_BATCH_FILE_SIZE = 50 * 1024 * 1024;

    @PostMapping("/upload")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ApiResponse<Map<String, List<Long>>> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile singleFile,
            @RequestParam(value = "files", required = false) List<MultipartFile> multiFiles) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            List<Long> fileIds = new ArrayList<>();

            if (singleFile != null && !singleFile.isEmpty()) {
                Long fileId = processSingleFile(singleFile, userId);
                fileIds.add(fileId);
            } else if (multiFiles != null && !multiFiles.isEmpty()) {
                List<Long> batchFileIds = processBatchFiles(multiFiles, userId);
                fileIds.addAll(batchFileIds);
            } else {
                return ApiResponse.error(400, "请选择要上传的文件");
            }

            Map<String, List<Long>> data = new HashMap<>();
            data.put("fileIds", fileIds);
            return ApiResponse.success(data);

        } catch (NotLoginException e) {
            return ApiResponse.error(401, "用户未登录");
        } catch (NotPermissionException e) {
            return ApiResponse.error(403, "权限不足，仅超级管理员可操作");
        } catch (MaxUploadSizeExceededException e) {
            return ApiResponse.error(400, "文件大小超出限制");
        } catch (MultipartException e) {
            return ApiResponse.error(400, "文件上传失败：" + e.getMessage());
        } catch (IOException e) {
            return ApiResponse.error(500, "文件写入失败：" + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ApiResponse<List<FileListDTO>> getFileList() {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            List<FileRecord> fileRecords = fileRepository.findAll();
            
            List<FileListDTO> fileList = fileRecords.stream()
                    .map(record -> new FileListDTO(
                            record.getId(),
                            record.getOriginalFilename(),
                            record.getUploadTime(),
                            record.getUploadTime(),
                            record.getFileSize(),
                            getMimeType(record.getFileType())
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

    @DeleteMapping("/{id}")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ApiResponse<Void> deleteFile(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Optional<FileRecord> fileRecordOpt = fileRepository.findById(id);
            
            if (fileRecordOpt.isEmpty()) {
                return ApiResponse.error(404, "文件不存在");
            }

            FileRecord fileRecord = fileRecordOpt.get();
            Path filePath = Paths.get(fileRecord.getStoragePath());
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            fileRepository.deleteById(id);
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

    @DeleteMapping("/batch")
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
                    Optional<FileRecord> fileRecordOpt = fileRepository.findById(id);
                    if (fileRecordOpt.isPresent()) {
                        FileRecord fileRecord = fileRecordOpt.get();
                        Path filePath = Paths.get(fileRecord.getStoragePath());
                        
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        }

                        fileRepository.deleteById(id);
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

    @GetMapping("/download/{id}")
    @SaCheckLogin
    @SaCheckPermission("3")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Optional<FileRecord> fileRecordOpt = fileRepository.findById(id);
            
            if (fileRecordOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            FileRecord fileRecord = fileRecordOpt.get();
            Path filePath = Paths.get(fileRecord.getStoragePath());
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath.toFile());
            String contentType = getMimeType(fileRecord.getFileType());
            
            logOperation(userId, id, "download", request.getRemoteAddr(), "success", null);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileRecord.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (NotLoginException | NotPermissionException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Long processSingleFile(MultipartFile file, Long userId) throws IOException {
        validateFileType(file.getOriginalFilename());
        validateFileSize(file.getSize(), MAX_SINGLE_FILE_SIZE, "单个文件");

        String storagePath = saveFile(file);
        Long fileId = saveFileRecord(file, userId, storagePath);
        logOperation(userId, fileId, "upload", null, "success", null);
        return fileId;
    }

    private List<Long> processBatchFiles(List<MultipartFile> files, Long userId) throws IOException {
        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        validateFileSize(totalSize, MAX_BATCH_FILE_SIZE, "批量文件");

        List<Long> fileIds = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                validateFileType(file.getOriginalFilename());
                String storagePath = saveFile(file);
                Long fileId = saveFileRecord(file, userId, storagePath);
                logOperation(userId, fileId, "upload", null, "success", null);
                fileIds.add(fileId);
            }
        }
        return fileIds;
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

    private Long saveFileRecord(MultipartFile file, Long userId, String storagePath) {
        FileRecord fileRecord = new FileRecord();
        fileRecord.setUserId(userId);
        fileRecord.setOriginalFilename(file.getOriginalFilename());
        fileRecord.setStoragePath(storagePath);
        fileRecord.setFileSize(file.getSize());
        fileRecord.setFileType(getFileExtension(file.getOriginalFilename()));
        fileRecord.setUploadTime(LocalDateTime.now());

        FileRecord savedRecord = fileRepository.save(fileRecord);
        return savedRecord.getId();
    }

    private void logOperation(Long userId, Long fileId, String operationType, String ipAddress, String status, String errorMessage) {
        FileOperationLog log = new FileOperationLog();
        log.setUserId(userId);
        log.setFileId(fileId);
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
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("png", "image/png");
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
}
