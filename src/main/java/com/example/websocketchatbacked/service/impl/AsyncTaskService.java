package com.example.websocketchatbacked.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncTaskService {

    @Async("chunkExecutor")
    public CompletableFuture<Void> processChunk(String documentId) {
        // 分块逻辑
        return CompletableFuture.runAsync(() -> {
            // 具体分块代码
        });
    }

    @Async("uploadExecutor")
    public CompletableFuture<Boolean> uploadFile(MultipartFile file) {
        // 上传逻辑，返回成功与否
        return CompletableFuture.completedFuture(true);
    }

    @Async("vectorExecutor")
    public CompletableFuture<Void> generateVector(String chunkId) {
        // 向量化逻辑
        return CompletableFuture.runAsync(() -> {
            // 具体向量化代码
        });
    }
}
