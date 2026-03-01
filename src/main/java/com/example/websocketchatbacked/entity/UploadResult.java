package com.example.websocketchatbacked.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传文件结果封装类
 * 用来记录每个文件的上传状态、文件名等信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult {
    // 文件名（包含后缀）
    private String fileName;
    // 文件唯一标识（可选，比如数据库记录ID）
    private String fileId;
    // 上传是否成功
    private boolean success;
    // 错误信息（如果上传失败，这里记录原因）
    private String errorMsg;
}
