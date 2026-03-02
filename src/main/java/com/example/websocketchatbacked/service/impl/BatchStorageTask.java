package com.example.websocketchatbacked.service.impl;

import com.example.websocketchatbacked.model.FileChunk;
import com.example.websocketchatbacked.service.BatchStorageService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
public class BatchStorageTask implements Callable<Boolean> {
    
    private final List<FileChunk> chunks;
    private final BatchStorageService batchStorageService;
    
    public BatchStorageTask(List<FileChunk> chunks, BatchStorageService batchStorageService) {
        this.chunks = chunks;
        this.batchStorageService = batchStorageService;
    }
    
    @Override
    public Boolean call() {
        try {
            log.info("开始批量存储: chunkCount={}", chunks.size());
            batchStorageService.batchSaveToMySQL(chunks);
            batchStorageService.batchIndexToElasticsearch(chunks);
            log.info("批量存储完成: chunkCount={}", chunks.size());
            return true;
        } catch (Exception e) {
            log.error("批量存储失败: chunkCount={}", chunks.size(), e);
            return false;
        }
    }
}