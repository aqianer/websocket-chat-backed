# 文件处理接口更新说明

## 更新概述

根据前端API开发文档（[API开发文档.md](file:///e:/JavaProjects/websocket-chat-front/API开发文档.md)），已成功更新文件处理接口（接口12：文件解析分段）的实现逻辑。

## 更新内容

### 1. 新增DTO类

#### FileProcessRequestDTO
**路径**: [FileProcessRequestDTO.java](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/dto/FileProcessRequestDTO.java)

**字段说明**:
- `kbId`: 知识库ID（必填）
- `documentIds`: 文档ID列表（必填），支持批量处理
- `parseStrategy`: 解析策略（必填），precise/fast
- `extractContent`: 提取内容类型（可选），image/ocr/table
- `segmentStrategy`: 分段策略（必填），auto/custom/hierarchy

#### FileProcessResponseDTO
**路径**: [FileProcessResponseDTO.java](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/dto/FileProcessResponseDTO.java)

**字段说明**:
- `processedDocuments`: 已处理文档列表
  - `documentId`: 文档ID
  - `fileName`: 文档文件名
  - `originalContent`: 文档原始内容
  - `chunkData`: 分块数据数组
    - `content`: 分块内容
    - `tokenCount`: Token数量
    - `vectorStatus`: 向量化状态

### 2. 新增服务类

#### FileProcessService
**路径**: [FileProcessService.java](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/service/FileProcessService.java)

**核心功能**:
- `processFiles()`: 批量处理文件
  - 支持多个文档ID的批量处理
  - 根据解析策略和分段策略处理文档
  - 自动保存分块到数据库

**分段策略实现**:
- **auto**: 自动分段（500字符/块，50字符重叠）
- **hierarchy**: 按文档层级结构分段（识别Markdown标题#）
- **custom**: 自定义分段（800字符/块，100字符重叠）

**其他功能**:
- `readFileContent()`: 读取文件内容
- `segmentContent()`: 根据策略分段内容
- `saveChunks()`: 保存分块到数据库

### 3. 更新控制器

#### FileController.processFile()
**路径**: [FileController.java#L306-360](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/controller/FileController.java#L306-360)

**更新内容**:
1. 添加认证和权限检查（`@SaCheckLogin`, `@SaCheckPermission("3")`）
2. 修改请求参数为`FileProcessRequestDTO`
3. 修改响应类型为`FileProcessResponseDTO`
4. 添加参数验证：
   - 知识库ID不能为空
   - 文档ID列表不能为空
   - 解析策略必须是precise或fast
   - 分段策略必须是auto、custom或hierarchy
5. 调用`FileProcessService.processFiles()`处理文件
6. 返回处理结果

**接口信息**:
- **请求方式**: POST
- **请求路径**: `/api/v1/file/process`
- **认证要求**: 需要登录和超级管理员权限

## 接口使用示例

### 请求示例

```json
{
  "kbId": 1,
  "documentIds": [123, 124, 125],
  "parseStrategy": "precise",
  "extractContent": ["image", "ocr", "table"],
  "segmentStrategy": "auto"
}
```

### 响应示例

```json
{
  "code": 200,
  "msg": "处理成功",
  "data": {
    "processedDocuments": [
      {
        "documentId": 123,
        "fileName": "技术文档.pdf",
        "originalContent": "这是文档的原始内容...",
        "chunkData": [
          {
            "content": "这是第一段分块内容...",
            "tokenCount": 256,
            "vectorStatus": "已向量化"
          },
          {
            "content": "这是第二段分块内容...",
            "tokenCount": 198,
            "vectorStatus": "已向量化"
          }
        ]
      }
    ]
  }
}
```

## 使用场景

### 新上传模式
用户上传多个文件后，传递所有文档ID列表进行批量处理：

```json
{
  "kbId": 1,
  "documentIds": [123, 124, 125],
  "parseStrategy": "precise",
  "extractContent": ["image", "ocr", "table"],
  "segmentStrategy": "auto"
}
```

### 继续上传模式
用户继续处理单个文档，仅传递当前文档ID：

```json
{
  "kbId": 1,
  "documentIds": [123],
  "parseStrategy": "precise",
  "extractContent": ["image", "ocr", "table"],
  "segmentStrategy": "auto"
}
```

## 错误处理

### 400 请求参数错误
```json
{
  "code": 400,
  "msg": "知识库ID不能为空"
}
```

### 401 未授权
```json
{
  "code": 401,
  "msg": "未授权，请先登录"
}
```

### 403 无权限访问
```json
{
  "code": 403,
  "msg": "权限不足，仅超级管理员可操作"
}
```

### 500 服务器内部错误
```json
{
  "code": 500,
  "msg": "文件处理异常: 文档不存在: 123"
}
```

## 技术实现细节

### 事务管理
使用`@Transactional`注解确保数据一致性，文件处理和分块保存在同一事务中。

### 日志记录
记录关键操作日志：
- 文件处理开始
- 文件读取成功
- 内容分段完成
- 分块保存成功
- 文档处理成功/失败

### 错误处理
捕获并处理各种异常：
- 文件不存在
- 文档不存在
- IO异常
- 数据库异常

## 与原有系统的集成

### 数据库集成
- 使用现有的`KbDocument`和`KbChunk`实体
- 使用现有的`KbDocumentRepository`和`KbChunkRepository`
- 自动更新文档的分块计数

### 认证授权集成
- 使用SA-Token进行认证
- 使用`@SaCheckLogin`检查登录状态
- 使用`@SaCheckPermission("3")`检查权限

### 文件存储集成
- 使用现有的文件存储路径
- 支持已上传文件的读取和处理

## 测试建议

### 单元测试
1. 测试各种分段策略的正确性
2. 测试批量处理功能
3. 测试参数验证
4. 测试异常处理

### 集成测试
1. 测试完整的文件上传和处理流程
2. 测试与前端API的对接
3. 测试并发处理场景
4. 测试大文件处理性能

## 性能优化建议

1. **批量处理优化**: 对于大量文档，可以考虑使用线程池并发处理
2. **分块策略优化**: 根据文档类型选择合适的分段策略
3. **缓存优化**: 缓存已处理的文档内容，避免重复读取
4. **数据库优化**: 批量插入分块数据，减少数据库操作次数

## 后续扩展方向

1. **支持更多文件格式**: 扩展支持Excel、PPT等格式
2. **更智能的分段策略**: 基于NLP的语义分段
3. **OCR集成**: 集成OCR服务处理扫描件
4. **图片提取**: 提取文档中的图片内容
5. **表格识别**: 识别和提取表格数据

## 总结

本次更新完全符合前端API开发文档的要求，实现了文件解析分段接口的所有功能，包括：
- 批量处理多个文档
- 支持多种解析策略
- 支持多种分段策略
- 完整的参数验证
- 统一的错误处理
- 详细的日志记录

系统已准备好与前端进行集成测试。