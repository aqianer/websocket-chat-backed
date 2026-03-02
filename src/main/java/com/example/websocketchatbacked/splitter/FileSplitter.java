package com.example.websocketchatbacked.splitter;

import java.util.List;

public interface FileSplitter {
    
    List<String> split(String content);
    
    String getStrategyName();
}