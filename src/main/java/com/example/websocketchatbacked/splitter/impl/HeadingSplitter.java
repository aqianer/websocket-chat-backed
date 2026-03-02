package com.example.websocketchatbacked.splitter.impl;

import com.example.websocketchatbacked.entity.MdParseResult;
import com.example.websocketchatbacked.entity.ParseResult;
import com.example.websocketchatbacked.splitter.FileSplitter;
import com.vladsch.flexmark.util.ast.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * MD 标题切分器
 *
 */
public class HeadingSplitter implements FileSplitter {

    private static final String STRATEGY_NAME = "heading";
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    @Override
    public List<String> split(ParseResult parseResult) {
        List<String> chunks = new ArrayList<>();
        // TODO
        Node node = null;
        if (parseResult instanceof MdParseResult) {
            // MD文件：获取Node节点遍历分块
            node = ((MdParseResult) parseResult).getAstNode();
        }
        node.walk(new NodeVisitor() {
        });
        return chunks;
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
}