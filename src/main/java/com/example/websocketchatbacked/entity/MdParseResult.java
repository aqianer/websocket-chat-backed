package com.example.websocketchatbacked.entity;

import com.vladsch.flexmark.util.ast.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MdParseResult implements ParseResult{
    private Node astNode;
}
