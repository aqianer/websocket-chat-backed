package com.example.websocketchatbacked.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class EsKbChunk {

    /**
     * 分块ID（对应ES的chunk_id，建议用KbChunk的id）
     */
    private String chunkId;

    /**
     * 文档ID（对应kb_document_id）
     */
    private Long kbDocumentId;

    /**
     * 分块内容（对应content，全文检索）
     */
    private String content;

    /**
     * 分块序号（对应chunk_num）
     */
    private Integer chunkNum;

    /**
     * 文件名（对应file_name，建议从Document中取）
     */
    private String fileName;

    /**
     * 创建时间（对应create_time）
     */
    private String createTime;

    // 工具方法：从KbChunk和Document构建EsKbChunk
    public static EsKbChunk from(KbChunk chunk, KbDocument document) {
        EsKbChunk esChunk = new EsKbChunk();
        esChunk.setChunkId(chunk.getId().toString()); // 假设KbChunk有id
        esChunk.setKbDocumentId(chunk.getDocId());
        esChunk.setContent(chunk.getContent());
        esChunk.setChunkNum(chunk.getChunkNum());
        esChunk.setFileName(document.getFileName()); // 从原文档取文件名
        esChunk.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return esChunk;
    }
}