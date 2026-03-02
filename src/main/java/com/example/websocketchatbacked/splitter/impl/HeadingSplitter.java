package com.example.websocketchatbacked.splitter.impl;

import com.example.websocketchatbacked.splitter.FileSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeadingSplitter implements FileSplitter {
    
    private static final String STRATEGY_NAME = "heading";
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    
    @Override
    public List<String> split(String content) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return chunks;
        }
        
        Matcher matcher = HEADING_PATTERN.matcher(content);
        int lastEnd = 0;
        String lastHeading = "";
        
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String chunkContent = content.substring(lastEnd, matcher.start()).trim();
                if (!chunkContent.isEmpty()) {
                    chunks.add(chunkContent);
                }
            }
            lastEnd = matcher.end();
            lastHeading = matcher.group(2);
        }
        
        if (lastEnd < content.length()) {
            String lastChunk = content.substring(lastEnd).trim();
            if (!lastChunk.isEmpty()) {
                chunks.add(lastChunk);
            }
        }
        
        if (chunks.isEmpty()) {
            chunks.add(content);
        }
        
        return chunks;
    }
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
}