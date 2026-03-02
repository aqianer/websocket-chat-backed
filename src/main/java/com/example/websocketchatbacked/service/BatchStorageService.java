package com.example.websocketchatbacked.service;

import com.example.websocketchatbacked.entity.KbChunk;
import com.example.websocketchatbacked.model.FileChunk;
import com.example.websocketchatbacked.repository.KbChunkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BatchStorageService {
    
    @Autowired
    private KbChunkRepository kbChunkRepository;
    
    @Autowired
    private ElasticsearchIndexService elasticsearchIndexService;
    
    private static final int BATCH_SIZE = 100;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Transactional
    public void batchSaveToMySQL(List<FileChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        
        List<KbChunk> kbChunks = new ArrayList<>();
        for (FileChunk chunk : chunks) {
            KbChunk kbChunk = new KbChunk();
            kbChunk.setDocId(chunk.getDocId());
            kbChunk.setKbId(chunk.getKbId());
            kbChunk.setContent(chunk.getContent());
            kbChunk.setChunkNum(chunk.getChunkNum());
            kbChunk.setMetadata(chunk.getMetadata());
            kbChunks.add(kbChunk);
        }
        
        List<List<KbChunk>> batches = partitionList(kbChunks, BATCH_SIZE);
        for (List<KbChunk> batch : batches) {
            try {
                kbChunkRepository.saveAll(batch);
                log.info("批量保存到MySQL成功: batchCount={}", batch.size());
            } catch (Exception e) {
                log.error("批量保存到MySQL失败: batchCount={}", batch.size(), e);
                throw e;
            }
        }
    }
    
    public void batchIndexToElasticsearch(List<FileChunk> chunks) throws IOException {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        
        List<List<FileChunk>> batches = partitionList(chunks, BATCH_SIZE);
        for (List<FileChunk> batch : batches) {
            try {
                elasticsearchIndexService.batchIndex(batch);
                log.info("批量索引到Elasticsearch成功: batchCount={}", batch.size());
            } catch (Exception e) {
                log.error("批量索引到Elasticsearch失败: batchCount={}", batch.size(), e);
                throw e;
            }
        }
    }
    
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }
    
    public void updateChunkWithEsDocId(Long chunkId, String esDocId) {
        try {
            KbChunk chunk = kbChunkRepository.findById(chunkId).orElse(null);
            if (chunk != null) {
                chunk.setEsDocId(esDocId);
                kbChunkRepository.save(chunk);
            }
        } catch (Exception e) {
            log.error("更新chunk的esDocId失败: chunkId={}", chunkId, e);
        }
    }
    
    public void deleteChunksByDocId(Long docId) {
        try {
            List<KbChunk> chunks = kbChunkRepository.findByDocIdOrderByChunkNum(docId);
            if (!chunks.isEmpty()) {
                kbChunkRepository.deleteAll(chunks);
                log.info("删除文档chunks成功: docId={}, count={}", docId, chunks.size());
            }
        } catch (Exception e) {
            log.error("删除文档chunks失败: docId={}", docId, e);
            throw e;
        }
    }
}