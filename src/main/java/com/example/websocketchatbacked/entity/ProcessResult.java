package com.example.websocketchatbacked.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件解析封装类
 * 用来记录每个文件解析的状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResult {
    /**
     * 文件id
     */
    private Long fileId;

    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 错误信息（如果失败，这里记录原因）
     */
    private String msg;

    /**
     * 分块结果
     */
    private List<KbChunk> chunkResult;

}
