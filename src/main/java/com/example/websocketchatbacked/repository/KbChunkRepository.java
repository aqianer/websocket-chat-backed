package com.example.websocketchatbacked.repository;

import com.example.websocketchatbacked.entity.KbChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KbChunkRepository extends JpaRepository<KbChunk, Long> {

    List<KbChunk> findByDocIdOrderByChunkNum(Long docId);

    @Query("SELECT c FROM KbChunk c WHERE c.docId = :docId ORDER BY c.chunkNum")
    List<KbChunk> findChunksByDocId(@Param("docId") Long docId);
}
