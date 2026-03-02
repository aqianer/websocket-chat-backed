package com.example.websocketchatbacked.parser.impl;

import com.example.websocketchatbacked.parser.FileParser;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class MarkdownParser implements FileParser {
    
    private static final String EXTENSION = "md";
    
    @Override
    public String parse(File file) throws IOException {
        String content = Files.readString(file.toPath());
        
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        
        return renderer.render(parser.parse(content));
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