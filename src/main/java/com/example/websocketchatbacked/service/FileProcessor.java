package com.example.websocketchatbacked.service;

import com.example.websocketchatbacked.factory.ParserFactory;
import com.example.websocketchatbacked.factory.SplitterFactory;
import com.example.websocketchatbacked.model.FileChunk;
import com.example.websocketchatbacked.parser.FileParser;
import com.example.websocketchatbacked.splitter.FileSplitter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileProcessor {
    
    @Autowired
    private ParserFactory parserFactory;
    
    @Autowired
    private SplitterFactory splitterFactory;
    
    public List<FileChunk> processFile(Long docId, Long kbId, String filePath, 
                                       String fileExtension, String splitStrategy) throws IOException {
        
        log.info("开始处理文件: docId={}, kbId={}, filePath={}, extension={}, strategy={}", 
                docId, kbId, filePath, fileExtension, splitStrategy);
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }
        
        FileParser parser = parserFactory.getParserByExtension(fileExtension);
        String content = parser.parse(file);
        
        log.info("文件解析完成: docId={}, contentLength={}", docId, content.length());
        
        FileSplitter splitter = splitterFactory.getSplitter(splitStrategy);
        List<String> chunkContents = splitter.split(content);
        
        log.info("文件分块完成: docId={}, chunkCount={}", docId, chunkContents.size());
        
        List<FileChunk> chunks = new ArrayList<>();
        for (int i = 0; i < chunkContents.size(); i++) {
            FileChunk chunk = new FileChunk(
                    docId,
                    kbId,
                    chunkContents.get(i),
                    i + 1
            );
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    public List<FileChunk> processFileWithCustomSplitter(Long docId, Long kbId, String filePath, 
                                                         String fileExtension, FileSplitter splitter) throws IOException {
        
        log.info("开始处理文件(自定义分块器): docId={}, kbId={}, filePath={}, extension={}", 
                docId, kbId, filePath, fileExtension);
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在: " + filePath);
        }
        
        FileParser parser = parserFactory.getParserByExtension(fileExtension);
        String content = parser.parse(file);
        
        log.info("文件解析完成: docId={}, contentLength={}", docId, content.length());
        
        List<String> chunkContents = splitter.split(content);
        
        log.info("文件分块完成: docId={}, chunkCount={}", docId, chunkContents.size());
        
        List<FileChunk> chunks = new ArrayList<>();
        for (int i = 0; i < chunkContents.size(); i++) {
            FileChunk chunk = new FileChunk(
                    docId,
                    kbId,
                    chunkContents.get(i),
                    i + 1
            );
            chunks.add(chunk);
        }
        
        return chunks;
    }
}