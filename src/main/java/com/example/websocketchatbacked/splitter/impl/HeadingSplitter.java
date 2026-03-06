package com.example.websocketchatbacked.splitter.impl;

import com.example.websocketchatbacked.entity.MdParseResult;
import com.example.websocketchatbacked.entity.ParseResult;
import com.example.websocketchatbacked.splitter.FileSplitter;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * MD 标题切分器
 *
 */
public class HeadingSplitter implements FileSplitter {

    private static final Logger logger = LoggerFactory.getLogger(HeadingSplitter.class);
    private static final String STRATEGY_NAME = "heading";
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    private List<String> chunks;
    private StringBuilder currentChunk;
    private int currentHeadingLevel;

    @Override
    public List<String> split(ParseResult parseResult) {
        List<String> chunks = new ArrayList<>();
        this.chunks = chunks;
        currentChunk = new StringBuilder();
        currentHeadingLevel = 0;

        if (!(parseResult instanceof MdParseResult)) {
            logger.warn("解析结果类型不匹配，期望 MdParseResult，实际: {}", parseResult.getClass().getSimpleName());
            return chunks;
        }

        Node node = ((MdParseResult) parseResult).getAstNode();
        if (node == null) {
            logger.warn("AST 节点为空");
            return chunks;
        }

        List<VisitHandler<?>> visitHandlers = new ArrayList<>();
        visitHandlers.add(new VisitHandler<>(Heading.class, this::handleHeading));
        visitHandlers.add(new VisitHandler<>(Text.class, this::handleText));

        NodeVisitor visitor = new NodeVisitor(visitHandlers.toArray(new VisitHandler[0]));
        visitor.visit(node);

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        logger.info("标题切分完成，共生成 {} 个分块", chunks.size());
        return chunks;
    }

    private void handleHeading(Heading heading) {
        int level = heading.getLevel();
        String headingText = heading.getText().toString();
        logger.debug("处理标题：级别 {}，内容 {}", level, headingText);

        if (currentChunk.length() > 0 && level <= currentHeadingLevel) {
            chunks.add(currentChunk.toString().trim());
            currentChunk = new StringBuilder();
        }

        for (int i = 0; i < level; i++) {
            currentChunk.append("#");
        }
        currentChunk.append(" ").append(headingText).append("\n\n");
        currentHeadingLevel = level;
    }

    private void handleText(Text text) {
        String content = text.getChars().toString().trim();
        if (!content.isEmpty()) {
            logger.debug("处理文本：{}", content);
            currentChunk.append(content).append("\n\n");
        }
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
}