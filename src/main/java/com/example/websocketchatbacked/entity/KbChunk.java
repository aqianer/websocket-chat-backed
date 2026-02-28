package com.example.websocketchatbacked.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "kb_chunk")
public class KbChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chunk_num", nullable = false)
    private Integer chunkNum;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "embedding_id", length = 255)
    private String embeddingId;

    @Column(name = "es_doc_id", length = 255)
    private String esDocId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_id", insertable = false, updatable = false)
    private KbDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kb_id", insertable = false, updatable = false)
    private KnowledgeBase knowledgeBase;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getChunkNum() {
        return chunkNum;
    }

    public void setChunkNum(Integer chunkNum) {
        this.chunkNum = chunkNum;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getEmbeddingId() {
        return embeddingId;
    }

    public void setEmbeddingId(String embeddingId) {
        this.embeddingId = embeddingId;
    }

    public String getEsDocId() {
        return esDocId;
    }

    public void setEsDocId(String esDocId) {
        this.esDocId = esDocId;
    }

    public KbDocument getDocument() {
        return document;
    }

    public void setDocument(KbDocument document) {
        this.document = document;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }
}
