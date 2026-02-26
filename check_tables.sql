-- 检查数据库中的所有表及其数据量
SELECT 
    TABLE_NAME as '表名',
    TABLE_ROWS as '记录数',
    ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) as '大小(MB)',
    TABLE_COMMENT as '表注释'
FROM 
    information_schema.TABLES 
WHERE 
    TABLE_SCHEMA = 'cbs'
ORDER BY 
    TABLE_NAME;
