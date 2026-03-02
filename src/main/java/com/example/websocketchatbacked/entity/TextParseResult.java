package com.example.websocketchatbacked.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TextParseResult implements ParseResult{
    private String text;
}
