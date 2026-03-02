package com.example.websocketchatbacked.parser;

import com.example.websocketchatbacked.entity.ParseResult;

import java.io.File;
import java.io.IOException;

public interface FileParser {
    
    ParseResult parse(String file) throws IOException;
    
    String getSupportedExtension();
    
    boolean supports(String fileExtension);
}