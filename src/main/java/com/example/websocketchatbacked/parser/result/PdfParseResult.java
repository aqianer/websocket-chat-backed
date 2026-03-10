package com.example.websocketchatbacked.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mapstruct.ap.shaded.freemarker.core.TextBlock;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfParseResult implements ParseResult{
    private List<PdfPage> pages;
}
