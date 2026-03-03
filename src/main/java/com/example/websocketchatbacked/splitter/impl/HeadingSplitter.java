package com.example.websocketchatbacked.splitter.impl;

import com.example.websocketchatbacked.entity.MdParseResult;
import com.example.websocketchatbacked.entity.ParseResult;
import com.example.websocketchatbacked.splitter.FileSplitter;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;

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

        // 1. 先校验类型，避免空指针
        if (!(parseResult instanceof MdParseResult)) {
            return chunks;
        }

        // 2. 获取 AST 节点（0.64.8 版本 getAstNode() 返回的是 Document 类型，属于 Node 子类）
        Node node = ((MdParseResult) parseResult).getAstNode();
        if (node == null) {
            return chunks;
        }
        // 步骤1：定义要访问的节点类型和处理逻辑（这里以 Heading 标题节点为例）
        List<VisitHandler<?>> visitHandlers = new ArrayList<>();
        // 处理标题节点
        visitHandlers.add(new VisitHandler<>(Heading.class, this::handleHeading));
        // 处理普通文本节点（可选，根据需求加）
        visitHandlers.add(new VisitHandler<>(Text.class, this::handleText));

        // 步骤2：创建 NodeVisitor（传入 VisitHandler 数组）
        NodeVisitor visitor = new NodeVisitor(visitHandlers.toArray(new VisitHandler[0]));

        // 步骤3：调用 walk 方法（传入 visitor）
//        node.walk(visitor);
        // 步骤3：调用 visitor.visit(node) 遍历 AST（关键修正！）
        visitor.visit(node);

        return chunks;
    }

    // 标题节点处理逻辑
    private void handleHeading(Heading heading) {
        // 获取标题级别（1-6级）
        int level = heading.getLevel();
        // 获取标题文本
        String headingText = heading.getText().toString();
        System.out.println("标题级别：" + level + "，标题内容：" + headingText);
        // 这里可以把标题作为分块的分隔符，往 chunks 里加内容
    }

    // 文本节点处理逻辑（可选）
    private void handleText(Text text) {
        String content = text.getChars().toString();
        System.out.println("文本内容：" + content);
        // 收集文本内容到对应的分块中
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
}