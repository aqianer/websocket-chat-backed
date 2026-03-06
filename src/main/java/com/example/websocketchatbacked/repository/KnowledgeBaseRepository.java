package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long>, JpaSpecificationExecutor<KnowledgeBase> {

    @Query("SELECT kb FROM KnowledgeBase kb JOIN kb.documentRelations dr WHERE dr.id.kbId = :kbId")
    KnowledgeBase findWithDocumentsByKbId(@Param("kbId") Long kbId);
}
