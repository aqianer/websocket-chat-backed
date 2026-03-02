package com.example.websocketchatbacked.parser.impl;

import com.example.websocketchatbacked.entity.MdParseResult;
import com.example.websocketchatbacked.entity.ParseResult;
import com.example.websocketchatbacked.parser.FileParser;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Markdown文件解析器
 * 负责将MD文件解析为AST抽象语法树，供后续分块(Chunk)和向量化使用
 *
 * @author YourName
 */
public class MarkdownParser implements FileParser {

    // 【常量】支持的文件后缀名
    private static final String EXTENSION = "md";

    // 【静态单例】解析器实例（类加载时初始化，线程安全）
    private static final Parser FLEXMARK_PARSER;

    // 静态代码块：初始化解析器配置
    static {
        MutableDataSet options = new MutableDataSet()
                // 启用GFM扩展：删除线、表格、任务列表
                .set(Parser.EXTENSIONS, Arrays.asList(
                        StrikethroughExtension.create(),
                        TablesExtension.create(),
                        TaskListExtension.create()
                ))
                // 设置兼容模式为标准CommonMark，保证解析稳定性
                ;

        FLEXMARK_PARSER = Parser.builder(options).build();
    }

    /**
     * 核心解析方法
     * 注意：此处读取全文件内容。若需支持GB级超大文件，需改为流式解析（flexmark也支持）。
     *
     * @param filePath MD文件路径
     * @return 包含AST节点的解析结果
     * @throws IOException IO异常
     */
    @Override
    public ParseResult parse(String filePath) throws IOException {
        Path path = Path.of(filePath);
        // 读取文件：使用UTF-8编码，这是Markdown的标准编码
        String mdContent = Files.readString(path, StandardCharsets.UTF_8);
        // 解析为AST并封装返回
        return new MdParseResult(FLEXMARK_PARSER.parse(mdContent));
    }

    /**
     * 【修复Bug】返回支持的文件后缀名
     * 供策略工厂类识别当前解析器处理哪种文件
     */
    @Override
    public String getSupportedExtension() {
        return EXTENSION; // 原代码返回""，此处修正为返回常量
    }

    /**
     * 校验是否支持该文件后缀
     *
     * @param fileExtension 不带点的后缀名（如 md, pdf）
     */
    @Override
    public boolean supports(String fileExtension) {
        // 防御性编程：防止传入null
        return fileExtension != null && EXTENSION.equalsIgnoreCase(fileExtension);
    }
}