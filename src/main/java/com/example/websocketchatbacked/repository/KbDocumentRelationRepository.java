package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.KbDocumentRelation;
import com.example.websocketchatbacked.entity.KbDocumentRelationId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface KbDocumentRelationRepository extends JpaRepository<KbDocumentRelation, KbDocumentRelationId> {

    @Query("SELECT r FROM KbDocumentRelation r JOIN FETCH r.document WHERE r.id.kbId = :kbId")
    Page<KbDocumentRelation> findByKbId(@Param("kbId") Long kbId, Pageable pageable);

    @Query("SELECT r FROM KbDocumentRelation r JOIN FETCH r.document WHERE r.id.kbId = :kbId AND r.document.fileName LIKE %:keyword%")
    Page<KbDocumentRelation> findByKbIdAndDocumentNameContaining(@Param("kbId") Long kbId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT r FROM KbDocumentRelation r JOIN FETCH r.document WHERE r.id.kbId = :kbId ORDER BY r.createTime DESC")
    Page<KbDocumentRelation> findByKbIdOrderByCreateTimeDesc(@Param("kbId") Long kbId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM KbDocumentRelation r WHERE r.id.kbId = :kbId AND r.id.documentId = :documentId")
    boolean existsByKbIdAndDocumentId(@Param("kbId") Long kbId, @Param("documentId") Long documentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM KbDocumentRelation r WHERE r.id.kbId = :kbId AND r.id.documentId = :documentId")
    void deleteByKbIdAndDocumentId(@Param("kbId") Long kbId, @Param("documentId") Long documentId);
}
