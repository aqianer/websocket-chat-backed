package com.example.websocketchatbacked.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.example.websocketchatbacked.dto.*;
import com.example.websocketchatbacked.entity.KbChunk;
import com.example.websocketchatbacked.entity.KbDocument;
import com.example.websocketchatbacked.entity.KnowledgeBase;
import com.example.websocketchatbacked.exception.BusinessException;
import com.example.websocketchatbacked.repository.KbChunkRepository;
import com.example.websocketchatbacked.repository.KbDocumentRepository;
import com.example.websocketchatbacked.repository.KnowledgeBaseRepository;
import com.example.websocketchatbacked.service.KnowledgeBaseService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired
    private KbDocumentRepository kbDocumentRepository;

    @Autowired
    private KbChunkRepository kbChunkRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public PageResponse<KnowledgeBaseDTO> getKnowledgeBaseList(Integer page, Integer pageSize, String department, String owner, String type) {
        int pageNum = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 10;

        Specification<KnowledgeBase> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (department != null && !department.isEmpty()) {
                predicates.add(cb.equal(root.get("department"), department));
            }
            if (owner != null && !owner.isEmpty()) {
                predicates.add(cb.equal(root.get("owner"), owner));
            }
            if (type != null && !type.isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(pageNum - 1, size);
        Page<KnowledgeBase> kbPage = knowledgeBaseRepository.findAll(spec, pageable);

        List<KnowledgeBaseDTO> dtoList = kbPage.getContent().stream().map(kb -> {
            KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
            dto.setId(kb.getId());
            dto.setName(kb.getName());
            dto.setOwner(kb.getOwner());
            dto.setDocCount(kb.getDocCount() != null ? kb.getDocCount() : 0);
            dto.setVectorDim(kb.getVectorDim());
            dto.setCreateTime(kb.getCreateTime() != null ? DATE_FORMATTER.format(kb.getCreateTime()) : "");
            dto.setStatus(kb.getStatus() != null && kb.getStatus() == 1 ? "正常" : "维护中");
            dto.setType(kb.getType());
            dto.setDepartment(kb.getDepartment());
            return dto;
        }).collect(Collectors.toList());

        return new PageResponse<>(dtoList, kbPage.getTotalElements());
    }

    @Override
    public KnowledgeBaseDTO getKnowledgeBaseById(Long id) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "知识库不存在"));

        KnowledgeBaseDTO dto = new KnowledgeBaseDTO();
        dto.setId(kb.getId());
        dto.setName(kb.getName());
        dto.setOwner(kb.getOwner());
        dto.setDocCount(kb.getDocCount() != null ? kb.getDocCount() : 0);
        dto.setVectorDim(kb.getVectorDim());
        dto.setCreateTime(kb.getCreateTime() != null ? DATE_FORMATTER.format(kb.getCreateTime()) : "");
        dto.setStatus(kb.getStatus() != null && kb.getStatus() == 1 ? "正常" : "维护中");
        dto.setType(kb.getType());
        dto.setDepartment(kb.getDepartment());
        return dto;
    }

    @Override
    @Transactional
    public void createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        if (request.getName() == null || request.getName().length() < 2 || request.getName().length() > 50) {
            throw new BusinessException(400, "知识库名称长度必须在2-50字符之间");
        }
        if (request.getType() == null || !List.of("tech", "business", "policy").contains(request.getType())) {
            throw new BusinessException(400, "知识库类型必须是tech/business/policy之一");
        }
        if (request.getOwner() == null || request.getOwner().length() < 2 || request.getOwner().length() > 20) {
            throw new BusinessException(400, "负责人姓名长度必须在2-20字符之间");
        }
        if (request.getVectorDim() == null || !List.of(768, 1024, 1536, 2048).contains(request.getVectorDim())) {
            throw new BusinessException(400, "向量维度必须是768/1024/1536/2048之一");
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setType(request.getType());
        kb.setOwner(request.getOwner());
        kb.setDepartment(request.getDepartment());
        kb.setVectorDim(request.getVectorDim());
        kb.setDocCount(0);
        kb.setStatus((byte) 1);
        kb.setDeleted((byte) 0);
        kb.setCreateTime(LocalDateTime.now());
        kb.setUpdateTime(LocalDateTime.now());

        knowledgeBaseRepository.save(kb);
    }

    @Override
    @Transactional
    public void updateKnowledgeBase(Long id, UpdateKnowledgeBaseRequest request) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "知识库不存在"));

        if (request.getName() != null) {
            if (request.getName().length() < 2 || request.getName().length() > 50) {
                throw new BusinessException(400, "知识库名称长度必须在2-50字符之间");
            }
            kb.setName(request.getName());
        }
        if (request.getType() != null) {
            if (!List.of("tech", "business", "policy").contains(request.getType())) {
                throw new BusinessException(400, "知识库类型必须是tech/business/policy之一");
            }
            kb.setType(request.getType());
        }
        if (request.getOwner() != null) {
            if (request.getOwner().length() < 2 || request.getOwner().length() > 20) {
                throw new BusinessException(400, "负责人姓名长度必须在2-20字符之间");
            }
            kb.setOwner(request.getOwner());
        }
        if (request.getDepartment() != null) {
            kb.setDepartment(request.getDepartment());
        }
        if (request.getVectorDim() != null) {
            if (!List.of(768, 1024, 1536, 2048).contains(request.getVectorDim())) {
                throw new BusinessException(400, "向量维度必须是768/1024/1536/2048之一");
            }
            kb.setVectorDim(request.getVectorDim());
        }

        kb.setUpdateTime(LocalDateTime.now());
        knowledgeBaseRepository.save(kb);
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(Long id) {
        if (!knowledgeBaseRepository.existsById(id)) {
            throw new BusinessException(404, "知识库不存在");
        }
        knowledgeBaseRepository.deleteById(id);
    }

    @Override
    public PageResponse<DocumentDTO> getDocumentList(Long kbId, Integer page, Integer pageSize, String keyword) {
        if (!knowledgeBaseRepository.existsById(kbId)) {
            throw new BusinessException(404, "知识库不存在");
        }

        int pageNum = page != null && page > 0 ? page : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum - 1, size);
        Page<KbDocument> docPage;

        if (keyword != null && !keyword.isEmpty()) {
            docPage = kbDocumentRepository.findByKbIdAndFileNameContaining(kbId, keyword, pageable);
        } else {
            docPage = kbDocumentRepository.findByKbId(kbId, pageable);
        }

        List<DocumentDTO> dtoList = docPage.getContent().stream().map(doc -> {
            DocumentDTO dto = new DocumentDTO();
            dto.setId(doc.getId());
            dto.setFileName(doc.getFileName());
            dto.setUploader(doc.getKnowledgeBase() != null ? doc.getKnowledgeBase().getOwner() : "系统");
            dto.setUploadTime(doc.getCreateTime() != null ? DATETIME_FORMATTER.format(doc.getCreateTime()) : "");
            dto.setChunkCount(doc.getChunkCount() != null ? doc.getChunkCount() : 0);
            dto.setVectorStatus(doc.getChunkCount() != null && doc.getChunkCount() > 0 ? "已向量化" : "未向量化");
            dto.setFileSize(formatFileSize(doc.getFileSize()));
            dto.setFileType(doc.getFileType());
            return dto;
        }).collect(Collectors.toList());

        return new PageResponse<>(dtoList, docPage.getTotalElements());
    }

    @Override
    @Transactional
    public UploadResultDTO uploadDocuments(Long kbId, MultipartFile[] files) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(404, "知识库不存在"));

        if (files == null || files.length == 0) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        if (files.length > 20) {
            throw new BusinessException(400, "批量上传最多支持20个文件");
        }

        int successCount = 0;
        int failCount = 0;

        for (MultipartFile file : files) {
            try {
                if (file.getSize() > 10 * 1024 * 1024) {
                    failCount++;
                    continue;
                }

                String originalFilename = file.getOriginalFilename();
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                if (!List.of("txt", "pdf", "doc", "docx").contains(fileExtension)) {
                    failCount++;
                    continue;
                }

                KbDocument doc = new KbDocument();
                doc.setKbId(kbId);
                doc.setUserId(StpUtil.getLoginIdAsLong());
                doc.setFileName(originalFilename);
                doc.setFileSize(file.getSize());
                doc.setFileType(fileExtension.toUpperCase());
                doc.setStoragePath("/uploads/kb/" + kbId + "/" + System.currentTimeMillis() + "_" + originalFilename);
                doc.setChunkCount(0);
                doc.setStatus((byte) 1);
                doc.setCurrentStep((byte) 1);
                doc.setCreateTime(java.time.LocalDateTime.now());
                doc.setUpdateTime(java.time.LocalDateTime.now());

                kbDocumentRepository.save(doc);
                successCount++;

                kb.setDocCount((kb.getDocCount() != null ? kb.getDocCount() : 0) + 1);
                knowledgeBaseRepository.save(kb);

            } catch (Exception e) {
                failCount++;
            }
        }

        return new UploadResultDTO(successCount, failCount);
    }

    @Override
    @Transactional
    public void deleteDocument(Long kbId, Long documentId) {
        KbDocument doc = kbDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));

        if (!doc.getKbId().equals(kbId)) {
            throw new BusinessException(403, "文档不属于该知识库");
        }

        kbDocumentRepository.deleteById(documentId);

        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId).orElse(null);
        if (kb != null && kb.getDocCount() != null && kb.getDocCount() > 0) {
            kb.setDocCount(kb.getDocCount() - 1);
            knowledgeBaseRepository.save(kb);
        }
    }

    @Override
    @Transactional
    public void reVectorizeDocument(Long kbId, Long documentId) {
        KbDocument doc = kbDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));

        if (!doc.getKbId().equals(kbId)) {
            throw new BusinessException(403, "文档不属于该知识库");
        }

        List<KbChunk> chunks = kbChunkRepository.findByDocIdOrderByChunkNum(documentId);
        for (KbChunk chunk : chunks) {
            chunk.setEmbeddingId(null);
            chunk.setEsDocId(null);
            kbChunkRepository.save(chunk);
        }
    }

    @Override
    public List<ChunkPreviewDTO> getDocumentChunks(Long kbId, Long documentId) {
        KbDocument doc = kbDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));

        if (!doc.getKbId().equals(kbId)) {
            throw new BusinessException(403, "文档不属于该知识库");
        }

        List<KbChunk> chunks = kbChunkRepository.findByDocIdOrderByChunkNum(documentId);
        return chunks.stream().map(chunk -> {
            ChunkPreviewDTO dto = new ChunkPreviewDTO();
            dto.setContent(chunk.getContent());
            dto.setTokenCount(chunk.getContent() != null ? chunk.getContent().length() / 2 : 0);
            dto.setVectorStatus(chunk.getEmbeddingId() != null ? "已向量化" : "未向量化");
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public DocumentUploadWizardDTO getDocumentUploadWizard(Long kbId, Long documentId) {
        KbDocument doc = kbDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));

        if (!doc.getKbId().equals(kbId)) {
            throw new BusinessException(403, "文档不属于该知识库");
        }

        DocumentUploadWizardDTO dto = new DocumentUploadWizardDTO();
        dto.setDocumentId(doc.getId());
        dto.setFileName(doc.getFileName());
        dto.setUploadTime(doc.getCreateTime() != null ? DATETIME_FORMATTER.format(doc.getCreateTime()) : "");
        dto.setFileSize(formatFileSize(doc.getFileSize()));
        dto.setFileType(doc.getFileType());

        List<KbChunk> chunks = kbChunkRepository.findByDocIdOrderByChunkNum(documentId);
        if (chunks == null || chunks.isEmpty()) {
            dto.setStatus("uploaded_not_chunked");
            dto.setChunkData(null);
        } else {
            dto.setStatus("chunked");
            List<ChunkPreviewDTO> chunkData = chunks.stream().map(chunk -> {
                ChunkPreviewDTO chunkDto = new ChunkPreviewDTO();
                chunkDto.setContent(chunk.getContent());
                chunkDto.setTokenCount(chunk.getContent() != null ? chunk.getContent().length() / 2 : 0);
                chunkDto.setVectorStatus(chunk.getEmbeddingId() != null ? "已向量化" : "未向量化");
                return chunkDto;
            }).collect(Collectors.toList());
            dto.setChunkData(chunkData);
        }

        return dto;
    }

    private String formatFileSize(Long size) {
        if (size == null) {
            return "0 B";
        }
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        }
    }
}
