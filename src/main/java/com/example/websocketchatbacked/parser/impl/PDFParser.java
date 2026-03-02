package com.example.websocketchatbacked.parser.impl;

import com.example.websocketchatbacked.parser.FileParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class PDFParser implements FileParser {
    
    private static final String EXTENSION = "pdf";
    
    @Override
    public String parse(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }
    
    @Override
    public String getSupportedExtension() {
        return EXTENSION;
    }
    
    @Override
    public boolean supports(String fileExtension) {
        return EXTENSION.equalsIgnoreCase(fileExtension);
    }
}