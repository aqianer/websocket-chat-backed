package com.example.websocketchatbacked.service;

import com.example.websocketchatbacked.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeBaseService {

    PageResponse<KnowledgeBaseDTO> getKnowledgeBaseList(Integer page, Integer pageSize, String department, String owner, String type);

    KnowledgeBaseDTO getKnowledgeBaseById(Long id);

    void createKnowledgeBase(CreateKnowledgeBaseRequest request);

    void updateKnowledgeBase(Long id, UpdateKnowledgeBaseRequest request);

    void deleteKnowledgeBase(Long id);

    PageResponse<DocumentDTO> getDocumentList(Long kbId, Integer page, Integer pageSize, String keyword);

    UploadResultDTO uploadDocuments(Long kbId, MultipartFile[] files);

    void deleteDocument(Long kbId, Long documentId);

    void reVectorizeDocument(Long kbId, Long documentId);

    List<ChunkPreviewDTO> getDocumentChunks(Long kbId, Long documentId);

    DocumentUploadWizardDTO getDocumentUploadWizard(Long kbId, Long documentId);
}
