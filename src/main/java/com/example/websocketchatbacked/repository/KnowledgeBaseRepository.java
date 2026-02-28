package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long>, JpaSpecificationExecutor<KnowledgeBase> {
}
