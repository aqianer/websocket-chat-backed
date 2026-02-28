-- 知识库表
CREATE TABLE `kb_knowledge_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(255) NOT NULL COMMENT '知识库名称',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '所属部门',
  `owner` VARCHAR(100) NOT NULL COMMENT '负责人',
  `type` VARCHAR(50) NOT NULL COMMENT '知识库类型',
  `doc_count` INT DEFAULT 0 COMMENT '文档数量',
  `vector_dim` INT DEFAULT NULL COMMENT '向量维度',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_name` (`name`),
  INDEX `idx_department` (`department`),
  INDEX `idx_owner` (`owner`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 文档表
CREATE TABLE `kb_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
  `storage_path` VARCHAR(500) DEFAULT NULL COMMENT '存储路径',
  `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_kb_id` (`kb_id`),
  INDEX `idx_file_name` (`file_name`),
  INDEX `idx_status` (`status`),
  INDEX `idx_create_time` (`create_time`),
  CONSTRAINT `fk_document_kb` FOREIGN KEY (`kb_id`) REFERENCES `kb_knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 文档分块表
CREATE TABLE `kb_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `doc_id` BIGINT NOT NULL COMMENT '文档ID',
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
  `content` TEXT NOT NULL COMMENT '分块内容',
  `chunk_num` INT NOT NULL COMMENT '分块序号',
  `metadata` JSON DEFAULT NULL COMMENT '元数据',
  `embedding_id` VARCHAR(255) DEFAULT NULL COMMENT '向量ID',
  `es_doc_id` VARCHAR(255) DEFAULT NULL COMMENT 'Elasticsearch文档ID',
  PRIMARY KEY (`id`),
  INDEX `idx_doc_id` (`doc_id`),
  INDEX `idx_kb_id` (`kb_id`),
  INDEX `idx_chunk_num` (`chunk_num`),
  INDEX `idx_embedding_id` (`embedding_id`),
  INDEX `idx_es_doc_id` (`es_doc_id`),
  CONSTRAINT `fk_chunk_doc` FOREIGN KEY (`doc_id`) REFERENCES `kb_document` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_chunk_kb` FOREIGN KEY (`kb_id`) REFERENCES `kb_knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块表';

-- 权限配置表
CREATE TABLE `kb_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permissions` VARCHAR(500) NOT NULL COMMENT '权限列表（逗号分隔）',
  PRIMARY KEY (`id`),
  INDEX `idx_kb_id` (`kb_id`),
  INDEX `idx_role_id` (`role_id`),
  CONSTRAINT `fk_permission_kb` FOREIGN KEY (`kb_id`) REFERENCES `kb_knowledge_base` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限配置表';
