package com.example.websocketchatbacked.parser.impl;

import com.example.websocketchatbacked.parser.FileParser;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class WordParser implements FileParser {
    
    private static final String DOC_EXTENSION = "doc";
    private static final String DOCX_EXTENSION = "docx";
    
    @Override
    public String parse(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        
        if (fileName.endsWith(DOCX_EXTENSION)) {
            return parseDocx(file);
        } else if (fileName.endsWith(DOC_EXTENSION)) {
            return parseDoc(file);
        }
        
        throw new IOException("Unsupported Word file format: " + fileName);
    }
    
    private String parseDocx(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                content.append(paragraph.getText()).append("\n");
            }
        }
        
        return content.toString();
    }
    
    private String parseDoc(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            
            return extractor.getText();
        }
    }
    
    @Override
    public String getSupportedExtension() {
        return DOCX_EXTENSION;
    }
    
    @Override
    public boolean supports(String fileExtension) {
        return DOCX_EXTENSION.equalsIgnoreCase(fileExtension) || DOC_EXTENSION.equalsIgnoreCase(fileExtension);
    }
}