package com.example.websocketchatbacked.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.websocketchatbacked.dto.*;
import com.example.websocketchatbacked.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-base")
@Tag(name = "知识库管理", description = "知识库管理相关接口")
@SaCheckLogin
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    @Operation(summary = "获取知识库列表", description = "获取知识库列表，支持分页和筛选")
    public ApiResponse<PageResponse<KnowledgeBaseDTO>> getKnowledgeBaseList(
            @Parameter(description = "页码，默认1") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页数量，默认10") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "部门筛选") @RequestParam(required = false) String department,
            @Parameter(description = "负责人筛选") @RequestParam(required = false) String owner,
            @Parameter(description = "类型筛选") @RequestParam(required = false) String type) {
        PageResponse<KnowledgeBaseDTO> result = knowledgeBaseService.getKnowledgeBaseList(page, pageSize, department, owner, type);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情", description = "获取指定知识库的详细信息")
    public ApiResponse<KnowledgeBaseDTO> getKnowledgeBaseById(
            @Parameter(description = "知识库ID", required = true) @PathVariable Long id) {
        KnowledgeBaseDTO result = knowledgeBaseService.getKnowledgeBaseById(id);
        return ApiResponse.success(result);
    }

    @PostMapping
    @Operation(summary = "创建知识库", description = "创建新的知识库")
    public ApiResponse<Void> createKnowledgeBase(
            @Parameter(description = "知识库信息", required = true) @RequestBody CreateKnowledgeBaseRequest request) {
        knowledgeBaseService.createKnowledgeBase(request);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识库", description = "更新知识库信息")
    public ApiResponse<Void> updateKnowledgeBase(
            @Parameter(description = "知识库ID", required = true) @PathVariable Long id,
            @Parameter(description = "知识库信息", required = true) @RequestBody UpdateKnowledgeBaseRequest request) {
        knowledgeBaseService.updateKnowledgeBase(id, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库", description = "删除指定的知识库")
    public ApiResponse<Void> deleteKnowledgeBase(
            @Parameter(description = "知识库ID", required = true) @PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{kbId}/documents")
    @Operation(summary = "获取知识库文档列表", description = "获取指定知识库的文档列表，支持分页和搜索")
    public ApiResponse<PageResponse<DocumentDTO>> getDocumentList(
            @Parameter(description = "知识库ID", required = true) @PathVariable Long kbId,
            @Parameter(description = "页码", required = true) @RequestParam Integer page,
            @Parameter(description = "每页数量", required = true) @RequestParam Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        PageResponse<DocumentDTO> result = knowledgeBaseService.getDocumentList(kbId, page, pageSize, keyword);
        return ApiResponse.success(result);
    }

    @PostMapping("/{kbId}/documents/upload")
    @Operation(summary = "上传文档", description = "向指定知识库上传文档")
    public ApiResponse<UploadResultDTO> uploadDocuments(
            @Parameter(description = "知识库ID", required = true) @PathVariable Long kbId,
            @Parameter(description = "文件列表", required = true) @RequestParam("files") MultipartFile[] files) {
        UploadResultDTO result = knowledgeBaseService.uploadDocuments(kbId, files);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{kbId}/documents/{documentId}")
    @Operation(summary = "删除文档", description = "从知识库中删除指定文档")
    public ApiResponse<Void> deleteDocument(
            @Parameter(description = "知识库ID", required = true) @PathVariable Long kbId,
            @Parameter(description = "文档ID", required = true) @PathVariable Long documentId) {
        knowledgeBaseService.deleteDocument(kbId, documentId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{kbId}/documents/{documentId}/re-vectorize")
    @Operation(summary = "重新向量化文档", description = "对指定文档重新进行向量化处理")
    public ApiResponse<Void> reVectorizeDocument(
            @Parameter(description = "知识库ID", required = true) @PathVariable Long kbId,
            @Parameter(description = "文档ID", required = true) @PathVariable Long documentId) {
        knowledgeBaseService.reVectorizeDocument(kbId, documentId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{kbId}/documents/{documentId}/chunks")
    @Operation(summary = "获取文档分块预览", description = "获取指定文档的分块信息和向量化状态")
    public ApiResponse<List<ChunkPreviewDTO>> getDocumentChunks(
            @Parameter(description = "知识库ID", required = true) @PathVariable Long kbId,
            @Parameter(description = "文档ID", required = true) @PathVariable Long documentId) {
        List<ChunkPreviewDTO> result = knowledgeBaseService.getDocumentChunks(kbId, documentId);
        return ApiResponse.success(result);
    }

    @PostMapping("/document-upload-wizard/{kbId}/{documentId}")
    @Operation(summary = "获取文档上传向导信息", description = "获取指定文档的详细信息，用于判断文档处理状态并导航到上传向导的相应步骤")
    public ApiResponse<DocumentUploadWizardDTO> getDocumentUploadWizard(
            @Parameter(description = "知识库ID", required = true) @PathVariable Long kbId,
            @Parameter(description = "文档ID", required = true) @PathVariable Long documentId) {
        DocumentUploadWizardDTO result = knowledgeBaseService.getDocumentUploadWizard(kbId, documentId);
        return ApiResponse.success(result);
    }
}
