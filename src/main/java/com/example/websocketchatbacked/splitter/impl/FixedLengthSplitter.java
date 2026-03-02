package com.example.websocketchatbacked.splitter.impl;

import com.example.websocketchatbacked.splitter.FileSplitter;

import java.util.ArrayList;
import java.util.List;

public class FixedLengthSplitter implements FileSplitter {
    
    private static final String STRATEGY_NAME = "fixed_length";
    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 50;
    
    private final int chunkSize;
    private final int overlap;
    
    public FixedLengthSplitter() {
        this(DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }
    
    public FixedLengthSplitter(int chunkSize, int overlap) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }
    
    @Override
    public List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return chunks;
        }
        
        String trimmedContent = content.trim();
        int length = trimmedContent.length();
        
        if (length <= chunkSize) {
            chunks.add(trimmedContent);
            return chunks;
        }
        
        int start = 0;
        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            
            if (end < length) {
                int lastSpace = trimmedContent.lastIndexOf(' ', end);
                int lastNewline = trimmedContent.lastIndexOf('\n', end);
                int breakPoint = Math.max(lastSpace, lastNewline);
                
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }
            
            String chunk = trimmedContent.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            start = end + overlap;
            if (start >= length - overlap) {
                start = length;
            }
        }
        
        return chunks;
    }
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    public int getChunkSize() {
        return chunkSize;
    }
    
    public int getOverlap() {
        return overlap;
    }
}