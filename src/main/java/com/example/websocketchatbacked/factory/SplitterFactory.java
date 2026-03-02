package com.example.websocketchatbacked.factory;

import com.example.websocketchatbacked.splitter.FileSplitter;
import com.example.websocketchatbacked.splitter.impl.FixedLengthSplitter;
import com.example.websocketchatbacked.splitter.impl.HeadingSplitter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SplitterFactory {
    
    private final Map<String, FileSplitter> splitters = new HashMap<>();
    
    public SplitterFactory() {
        registerSplitter(new HeadingSplitter());
        registerSplitter(new FixedLengthSplitter());
    }
    
    public void registerSplitter(FileSplitter splitter) {
        splitters.put(splitter.getStrategyName().toLowerCase(), splitter);
    }
    
    public FileSplitter getSplitter(String strategyName) {
        FileSplitter splitter = splitters.get(strategyName.toLowerCase());
        if (splitter == null) {
            throw new IllegalArgumentException("Unsupported split strategy: " + strategyName);
        }
        return splitter;
    }
    
    public FileSplitter getFixedLengthSplitter(int chunkSize, int overlap) {
        FileSplitter splitter = new FixedLengthSplitter(chunkSize, overlap);
        splitters.put(splitter.getStrategyName().toLowerCase(), splitter);
        return splitter;
    }
    
    public boolean supports(String strategyName) {
        return splitters.containsKey(strategyName.toLowerCase());
    }
}