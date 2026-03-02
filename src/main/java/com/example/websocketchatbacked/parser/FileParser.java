package com.example.websocketchatbacked.parser;

import java.io.File;
import java.io.IOException;

public interface FileParser {
    
    String parse(File file) throws IOException;
    
    String getSupportedExtension();
    
    boolean supports(String fileExtension);
}