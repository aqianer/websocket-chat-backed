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

    @Query("SELECT d FROM KbDocument d WHERE d.fileName LIKE %:keyword%")
    Page<KbDocument> findByFileNameContaining(@Param("keyword") String keyword, Pageable pageable);

    Optional<KbDocument> findByFileHash(String fileHash);
}
