package com.example.websocketchatbacked.entity;

import com.example.websocketchatbacked.dto.UploadedFileDTO;
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
    private UploadedFileDTO uploadedFile;
    // 上传是否成功
    private boolean success;
    // 错误信息（如果上传失败，这里记录原因）
    private String errorMsg;
}
