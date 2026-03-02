package com.example.websocketchatbacked.service;

import com.example.websocketchatbacked.model.FileChunk;
import com.example.websocketchatbacked.service.impl.FileProcessingTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
public class FileProcessingCoordinator {
    
    @Autowired
    private FileProcessor fileProcessor;
    
    @Autowired
    private BatchStorageService batchStorageService;
    
    @Autowired
    @Qualifier("fileProcessingExecutor")
    private Executor fileProcessingExecutor;
    
    @Autowired
    @Qualifier("batchStorageExecutor")
    private Executor batchStorageExecutor;
    
    @Autowired
    @Qualifier("esIndexExecutor")
    private Executor esIndexExecutor;
    
    public FileProcessingResult processFileAsync(Long docId, Long kbId, String filePath, 
                                                  String fileExtension, String splitStrategy) {
        
        log.info("异步处理文件: docId={}, kbId={}, filePath={}", docId, kbId, filePath);
        
        CountDownLatch latch = new CountDownLatch(1);
        FileProcessingTask task = new FileProcessingTask(docId, kbId, filePath, fileExtension, 
                splitStrategy, fileProcessor, latch);
        
        Future<Boolean> future = ((ThreadPoolExecutor) fileProcessingExecutor).submit(task);
        
        return new FileProcessingResult(future, latch);
    }
    
    public List<FileProcessingResult> processFilesAsync(List<FileProcessingRequest> requests) {
        
        log.info("批量异步处理文件: count={}", requests.size());
        
        CountDownLatch globalLatch = new CountDownLatch(requests.size());
        List<FileProcessingResult> results = new ArrayList<>();
        
        for (FileProcessingRequest request : requests) {
            FileProcessingTask task = new FileProcessingTask(
                    request.getDocId(),
                    request.getKbId(),
                    request.getFilePath(),
                    request.getFileExtension(),
                    request.getSplitStrategy(),
                    fileProcessor,
                    globalLatch
            );
            
            Future<Boolean> future = ((ThreadPoolExecutor) fileProcessingExecutor).submit(task);
            results.add(new FileProcessingResult(future, globalLatch));
        }
        
        return results;
    }
    
    public boolean processFileSync(Long docId, Long kbId, String filePath, 
                                    String fileExtension, String splitStrategy) {
        
        log.info("同步处理文件: docId={}, kbId={}, filePath={}", docId, kbId, filePath);
        
        try {
            List<FileChunk> chunks = fileProcessor.processFile(docId, kbId, filePath, 
                    fileExtension, splitStrategy);
            
            batchStorageService.batchSaveToMySQL(chunks);
            batchStorageService.batchIndexToElasticsearch(chunks);
            
            log.info("同步处理文件完成: docId={}, chunkCount={}", docId, chunks.size());
            return true;
        } catch (Exception e) {
            log.error("同步处理文件失败: docId={}", docId, e);
            return false;
        }
    }
    
    public boolean batchProcessFilesSync(List<FileProcessingRequest> requests) {
        
        log.info("批量同步处理文件: count={}", requests.size());
        
        List<FileChunk> allChunks = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        
        for (FileProcessingRequest request : requests) {
            try {
                List<FileChunk> chunks = fileProcessor.processFile(
                        request.getDocId(),
                        request.getKbId(),
                        request.getFilePath(),
                        request.getFileExtension(),
                        request.getSplitStrategy()
                );
                
                allChunks.addAll(chunks);
                successCount++;
            } catch (Exception e) {
                log.error("处理文件失败: docId={}", request.getDocId(), e);
                failCount++;
            }
        }
        
        log.info("文件处理完成: success={}, fail={}, totalChunks={}", 
                successCount, failCount, allChunks.size());
        
        if (!allChunks.isEmpty()) {
            try {
                batchStorageService.batchSaveToMySQL(allChunks);
                batchStorageService.batchIndexToElasticsearch(allChunks);
                return true;
            } catch (Exception e) {
                log.error("批量存储失败", e);
                return false;
            }
        }
        
        return failCount == 0;
    }
    
    public void waitForCompletion(List<FileProcessingResult> results, long timeout, TimeUnit unit) 
            throws InterruptedException {
        
        for (FileProcessingResult result : results) {
            result.getLatch().await(timeout, unit);
        }
    }
    
    public static class FileProcessingRequest {
        private Long docId;
        private Long kbId;
        private String filePath;
        private String fileExtension;
        private String splitStrategy;
        
        public FileProcessingRequest(Long docId, Long kbId, String filePath, 
                                      String fileExtension, String splitStrategy) {
            this.docId = docId;
            this.kbId = kbId;
            this.filePath = filePath;
            this.fileExtension = fileExtension;
            this.splitStrategy = splitStrategy;
        }
        
        public Long getDocId() { return docId; }
        public Long getKbId() { return kbId; }
        public String getFilePath() { return filePath; }
        public String getFileExtension() { return fileExtension; }
        public String getSplitStrategy() { return splitStrategy; }
    }
    
    public static class FileProcessingResult {
        private final Future<Boolean> future;
        private final CountDownLatch latch;
        
        public FileProcessingResult(Future<Boolean> future, CountDownLatch latch) {
            this.future = future;
            this.latch = latch;
        }
        
        public Future<Boolean> getFuture() { return future; }
        public CountDownLatch getLatch() { return latch; }
        
        public boolean isDone() {
            return future.isDone();
        }
        
        public Boolean get() throws InterruptedException, ExecutionException {
            return future.get();
        }
        
        public Boolean get(long timeout, TimeUnit unit) 
                throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }
    }
}