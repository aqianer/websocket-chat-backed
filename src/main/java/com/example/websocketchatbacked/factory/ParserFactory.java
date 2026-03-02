package com.example.websocketchatbacked.factory;

import com.example.websocketchatbacked.parser.FileParser;
import com.example.websocketchatbacked.parser.impl.MarkdownParser;
import com.example.websocketchatbacked.parser.impl.PDFParser;
import com.example.websocketchatbacked.parser.impl.WordParser;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ParserFactory {
    
    private final Map<String, FileParser> parsers = new HashMap<>();
    
    public ParserFactory() {
        registerParser(new MarkdownParser());
        registerParser(new WordParser());
        registerParser(new PDFParser());
    }
    
    public void registerParser(FileParser parser) {
        parsers.put(parser.getSupportedExtension().toLowerCase(), parser);
    }
    
    public FileParser getParser(String fileExtension) {
        FileParser parser = parsers.get(fileExtension.toLowerCase());
        if (parser == null) {
            throw new IllegalArgumentException("Unsupported file extension: " + fileExtension);
        }
        return parser;
    }
    
    public FileParser getParserByExtension(String fileExtension) {
        for (FileParser parser : parsers.values()) {
            if (parser.supports(fileExtension)) {
                return parser;
            }
        }
        throw new IllegalArgumentException("Unsupported file extension: " + fileExtension);
    }
    
    public boolean supports(String fileExtension) {
        return parsers.values().stream().anyMatch(parser -> parser.supports(fileExtension));
    }
}