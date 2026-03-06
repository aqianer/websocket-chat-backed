package com.example.websocketchatbacked.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "kb_document_relation")
public class KbDocumentRelation {

    @EmbeddedId
    private KbDocumentRelationId id;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("kbId")
    @JoinColumn(name = "kb_id")
    private KnowledgeBase knowledgeBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("documentId")
    @JoinColumn(name = "document_id")
    private KbDocument document;

    public KbDocumentRelation() {
    }

    public KbDocumentRelation(Long kbId, Long documentId) {
        this.id = new KbDocumentRelationId(kbId, documentId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KbDocumentRelation)) return false;
        KbDocumentRelation that = (KbDocumentRelation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public KbDocumentRelationId getId() {
        return id;
    }

    public void setId(KbDocumentRelationId id) {
        this.id = id;
    }

    public Long getKbId() {
        return id != null ? id.getKbId() : null;
    }

    public void setKbId(Long kbId) {
        if (this.id == null) {
            this.id = new KbDocumentRelationId();
        }
        this.id.setKbId(kbId);
    }

    public Long getDocumentId() {
        return id != null ? id.getDocumentId() : null;
    }

    public void setDocumentId(Long documentId) {
        if (this.id == null) {
            this.id = new KbDocumentRelationId();
        }
        this.id.setDocumentId(documentId);
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public KbDocument getDocument() {
        return document;
    }

    public void setDocument(KbDocument document) {
        this.document = document;
    }
}
