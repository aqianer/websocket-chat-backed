-- 查看package_detail表的结构和约束
SHOW CREATE TABLE package_detail;

-- 查看resource_type字段的CHECK约束
SELECT * FROM information_schema.CHECK_CONSTRAINTS 
WHERE CONSTRAINT_SCHEMA = 'cbs' 
AND TABLE_NAME = 'package_detail';

-- 查看所有表的记录数
SELECT 
    TABLE_NAME as '表名',
    TABLE_ROWS as '记录数'
FROM 
    information_schema.TABLES 
WHERE 
    TABLE_SCHEMA = 'cbs'
ORDER BY 
    TABLE_NAME;
