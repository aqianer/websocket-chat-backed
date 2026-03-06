-- 数据库迁移脚本：确保 kb_document_relation 表结构正确
-- 创建时间：2026-03-06
-- 目的：修复表结构，确保与实体类匹配

-- 检查表是否存在，如果不存在则创建
CREATE TABLE IF NOT EXISTS kb_document_relation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    kb_id BIGINT NOT NULL COMMENT '知识库ID',
    doc_id BIGINT NOT NULL COMMENT '文档ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_kb_id (kb_id),
    INDEX idx_doc_id (doc_id),
    INDEX idx_create_time (create_time),
    UNIQUE KEY uk_kb_doc (kb_id, doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档关联表';

-- 验证表结构
SELECT
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'cbs' AND TABLE_NAME = 'kb_document_relation'
ORDER BY ORDINAL_POSITION;

-- 检查索引
SELECT
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = 'cbs' AND TABLE_NAME = 'kb_document_relation'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;
