package com.example.websocketchatbacked.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class KbDocumentRelationId implements Serializable {

    @Column(name = "kb_id")
    private Long kbId;

    @Column(name = "document_id")
    private Long documentId;

    public KbDocumentRelationId() {
    }

    public KbDocumentRelationId(Long kbId, Long documentId) {
        this.kbId = kbId;
        this.documentId = documentId;
    }

    public Long getKbId() {
        return kbId;
    }

    public void setKbId(Long kbId) {
        this.kbId = kbId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KbDocumentRelationId)) return false;
        KbDocumentRelationId that = (KbDocumentRelationId) o;
        return Objects.equals(kbId, that.kbId) && Objects.equals(documentId, that.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kbId, documentId);
    }
}