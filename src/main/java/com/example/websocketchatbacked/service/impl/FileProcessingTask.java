//package com.example.websocketchatbacked.service.impl;
//
//import com.example.websocketchatbacked.model.FileChunk;
//import com.example.websocketchatbacked.service.FileProcessor;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.List;
//import java.util.concurrent.Callable;
//import java.util.concurrent.CountDownLatch;
//
//@Slf4j
//public class FileProcessingTask implements Callable<Boolean> {
//
//    private final Long docId;
//    private final Long kbId;
//    private final String filePath;
//    private final String fileExtension;
//    private final String splitStrategy;
//    private final FileProcessor fileProcessor;
//    private final CountDownLatch latch;
//
//    public FileProcessingTask(Long docId, Long kbId, String filePath, String fileExtension,
//                              String splitStrategy, FileProcessor fileProcessor, CountDownLatch latch) {
//        this.docId = docId;
//        this.kbId = kbId;
//        this.filePath = filePath;
//        this.fileExtension = fileExtension;
//        this.splitStrategy = splitStrategy;
//        this.fileProcessor = fileProcessor;
//        this.latch = latch;
//    }
//
//    @Override
//    public Boolean call() {
//        try {
//            log.info("开始处理文件: docId={}, filePath={}", docId, filePath);
//            List<FileChunk> chunks = fileProcessor.processFile(docId, kbId, filePath, fileExtension, splitStrategy);
//            log.info("文件处理完成: docId={}, chunkCount={}", docId, chunks.size());
//            return true;
//        } catch (Exception e) {
//            log.error("文件处理失败: docId={}, filePath={}", docId, filePath, e);
//            return false;
//        } finally {
//            if (latch != null) {
//                latch.countDown();
//            }
//        }
//    }
//}