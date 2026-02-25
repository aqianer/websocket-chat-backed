-- 创建数据库
CREATE DATABASE IF NOT EXISTS chat_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE chat_system;

-- 创建用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    authority INT COMMENT '权限：1=只写，2=只读，3=读写',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 创建文件记录表
CREATE TABLE IF NOT EXISTS file_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型（扩展名）',
    upload_time DATETIME NOT NULL COMMENT '上传时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_upload_time (upload_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件记录表';

-- 创建文件操作日志表
CREATE TABLE IF NOT EXISTS file_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    file_id BIGINT COMMENT '文件ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型（upload/download/delete）',
    operation_time DATETIME NOT NULL COMMENT '操作时间',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    status VARCHAR(20) NOT NULL COMMENT '操作状态（success/failed）',
    error_message TEXT COMMENT '错误信息',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_file_id (file_id),
    INDEX idx_operation_time (operation_time),
    INDEX idx_operation_type (operation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件操作日志表';

-- 插入测试用户数据
INSERT INTO sys_user (username, password, nickname, authority) VALUES
('admin', 'admin123', '管理员', 3),
('writer', 'writer123', '写权限用户', 1),
('reader', 'reader123', '读权限用户', 2);
