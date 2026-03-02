# 多格式文件处理系统

## 系统概述

本系统实现了基于策略模式和工厂模式的多格式文件处理功能，支持Markdown、Word、PDF三种格式的文件解析，提供按标题层级和固定长度两种分块策略，并支持将分块结果批量存储至MySQL和Elasticsearch。

## 技术栈

- **框架**: Spring Boot 3.5.0
- **Java版本**: Java 17
- **数据库**: MySQL (Spring Data JPA)
- **搜索引擎**: Elasticsearch 8.x
- **文件解析库**:
  - Markdown: flexmark-all 0.64.8
  - Word: Apache POI 5.2.4
  - PDF: Apache PDFBox 2.0.29

## 核心功能

### 1. 文件解析

支持三种文件格式的解析：

- **Markdown解析器** ([MarkdownParser](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/parser/impl/MarkdownParser.java)): 使用flexmark库解析Markdown文件，转换为HTML格式
- **Word解析器** ([WordParser](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/parser/impl/WordParser.java)): 使用Apache POI解析Word文件，支持.doc和.docx格式
- **PDF解析器** ([PDFParser](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/parser/impl/PDFParser.java)): 使用Apache PDFBox解析PDF文件

### 2. 文件分块

提供两种分块策略：

- **按标题分块** ([HeadingSplitter](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/splitter/impl/HeadingSplitter.java)): 根据Markdown标题符号（#）进行分块，保持内容的语义完整性
- **固定长度分块** ([FixedLengthSplitter](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/splitter/impl/FixedLengthSplitter.java)): 按指定字符长度分块，支持重叠策略（默认500字符分块，50字符重叠）

### 3. 设计模式应用

#### 策略模式

- **FileParser接口** ([FileParser](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/parser/FileParser.java)): 定义文件解析标准方法
- **FileSplitter接口** ([FileSplitter](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/splitter/FileSplitter.java)): 定义文件分块标准方法

#### 工厂模式

- **ParserFactory** ([ParserFactory](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/factory/ParserFactory.java)): 管理不同格式解析器的创建
- **SplitterFactory** ([SplitterFactory](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/factory/SplitterFactory.java)): 管理不同分块策略的创建

### 4. 并发处理

配置了三个专用线程池：

- **fileProcessingExecutor** ([FileProcessingThreadPoolConfig](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/config/FileProcessingThreadPoolConfig.java)): 文件处理线程池
  - 核心线程数: CPU核心数
  - 最大线程数: CPU核心数 * 2
  - 队列容量: 200

- **batchStorageExecutor**: 批量存储线程池
  - 核心线程数: CPU核心数
  - 最大线程数: CPU核心数 * 2
  - 队列容量: 150

- **esIndexExecutor**: ES索引线程池
  - 核心线程数: CPU核心数 / 2
  - 最大线程数: CPU核心数
  - 队列容量: 100

### 5. 数据存储

#### MySQL批量存储

**BatchStorageService** ([BatchStorageService](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/service/BatchStorageService.java)):

- 支持事务管理
- 分批处理（默认每批100条）
- 自动更新文档分块计数

#### Elasticsearch批量索引

**ElasticsearchIndexService** ([ElasticsearchIndexService](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/service/ElasticsearchIndexService.java)):

- 自动创建索引
- 批量索引操作（默认每批100条）
- 支持按文档ID和知识库ID删除索引

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    FileProcessingController                   │
│                    (REST API接口层)                            │
└────────────────────┬────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────────┐
│                 FileProcessingCoordinator                     │
│                    (任务协调层)                                │
└──────┬──────────────────────────────┬───────────────────────┘
       │                              │
┌──────▼──────────┐          ┌────────▼──────────────┐
│ FileProcessor    │          │ BatchStorageService   │
│ (文件处理层)      │          │ (批量存储层)           │
└──────┬───────────┘          └────────┬──────────────┘
       │                               │
┌──────▼──────────┐          ┌────────▼──────────────┐
│ ParserFactory   │          │ ElasticsearchIndex    │
│ SplitterFactory │          │ Service                │
│ (工厂模式)       │          │ (ES索引服务)           │
└──────┬──────────┘          └───────────────────────┘
       │
┌──────▼──────────┐
│ 具体解析器:       │
│ - MarkdownParser│
│ - WordParser    │
│ - PDFParser     │
│                 │
│ 具体分块器:       │
│ - HeadingSplitter│
│ - FixedLength   │
│   Splitter       │
└─────────────────┘
```

## API接口

### 1. 同步处理单个文件

**POST** `/api/file-processing/process`

请求体:
```json
{
  "docId": 1,
  "kbId": 1,
  "filePath": "/path/to/file.md",
  "fileExtension": "md",
  "splitStrategy": "heading"
}
```

### 2. 异步处理单个文件

**POST** `/api/file-processing/process-async`

请求体: 同上

### 3. 批量处理文件

**POST** `/api/file-processing/batch-process`

请求体:
```json
[
  {
    "docId": 1,
    "kbId": 1,
    "filePath": "/path/to/file1.md",
    "fileExtension": "md",
    "splitStrategy": "heading"
  },
  {
    "docId": 2,
    "kbId": 1,
    "filePath": "/path/to/file2.pdf",
    "fileExtension": "pdf",
    "splitStrategy": "fixed_length"
  }
]
```

### 4. 自定义分块参数

**POST** `/api/file-processing/process-custom`

请求体:
```json
{
  "docId": 1,
  "kbId": 1,
  "filePath": "/path/to/file.md",
  "fileExtension": "md",
  "splitStrategy": "fixed_length",
  "chunkSize": 1000,
  "overlap": 100
}
```

## 使用示例

### 1. 同步处理Markdown文件

```java
@Autowired
private FileProcessingCoordinator coordinator;

boolean success = coordinator.processFileSync(
    1L,  // docId
    1L,  // kbId
    "/path/to/document.md",
    "md",
    "heading"  // 按标题分块
);
```

### 2. 异步处理多个文件

```java
List<FileProcessingCoordinator.FileProcessingRequest> requests = Arrays.asList(
    new FileProcessingCoordinator.FileProcessingRequest(1L, 1L, "/path/to/file1.md", "md", "heading"),
    new FileProcessingCoordinator.FileProcessingRequest(2L, 1L, "/path/to/file2.pdf", "pdf", "fixed_length")
);

List<FileProcessingCoordinator.FileProcessingResult> results = coordinator.processFilesAsync(requests);
```

### 3. 使用自定义分块器

```java
@Autowired
private FileProcessor fileProcessor;

FixedLengthSplitter splitter = new FixedLengthSplitter(1000, 100);
List<FileChunk> chunks = fileProcessor.processFileWithCustomSplitter(
    1L, 1L, "/path/to/file.md", "md", splitter
);
```

## 配置说明

### Elasticsearch配置

在 `application.yml` 中配置:

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

### 线程池配置

线程池参数在 [FileProcessingThreadPoolConfig](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/config/FileProcessingThreadPoolConfig.java) 中配置，可根据服务器资源调整。

## 扩展性

### 添加新的文件格式解析器

1. 实现 [FileParser](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/parser/FileParser.java) 接口
2. 在 [ParserFactory](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/factory/ParserFactory.java) 中注册

### 添加新的分块策略

1. 实现 [FileSplitter](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/splitter/FileSplitter.java) 接口
2. 在 [SplitterFactory](file:///e:/JavaProjects/websocket-chat-backed/src/main/java/com/example/websocketchatbacked/fileparser/factory/SplitterFactory.java) 中注册

## 性能优化建议

1. **批量大小**: 默认每批100条，可根据实际性能调整
2. **线程池配置**: 根据服务器CPU核心数和内存大小调整线程池参数
3. **分块策略**: 对于大文件，建议使用固定长度分块以获得更好的性能
4. **并发控制**: 使用异步处理时，注意控制并发任务数量，避免资源耗尽

## 注意事项

1. 文件路径必须是绝对路径
2. 确保文件存在且有读取权限
3. MySQL和Elasticsearch必须先启动
4. 批量处理时注意事务大小，避免长时间占用连接

## 依赖说明

所有依赖已在项目的 [pom.xml](file:///e:/JavaProjects/websocket-chat-backed/pom.xml) 中配置，无需额外安装。

## 总结

本系统完整实现了多格式文件处理的核心功能，采用策略模式和工厂模式确保了良好的可扩展性和可维护性，通过线程池实现了高效的并发处理，支持MySQL和Elasticsearch双存储，为知识库管理提供了强大的文件处理能力。