package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.KbDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KbDocumentRepository extends JpaRepository<KbDocument, Long>, JpaSpecificationExecutor<KbDocument> {

    Page<KbDocument> findByKbId(Long kbId, Pageable pageable);

    @Query("SELECT d FROM KbDocument d WHERE d.kbId = :kbId AND d.fileName LIKE %:keyword%")
    Page<KbDocument> findByKbIdAndFileNameContaining(@Param("kbId") Long kbId, @Param("keyword") String keyword, Pageable pageable);

    List<KbDocument> findByKbId(Long kbId);
}
