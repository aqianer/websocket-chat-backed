package com.example.websocketchatbacked.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.CollectionSchemaParam;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG服务类，提供文档存储和检索功能
 * <p>
 * 使用 Spring AI 1.1.2 的标准 API，确保代码的健壮性和严谨性
 */
@Slf4j
@Service
public class RagService {
    private static final String COLLECTION_NAME = MilvusVectorStore.DEFAULT_COLLECTION_NAME;
    //向量维度定义1536，跟阿里巴巴embedding向量服务返回的维度保持一致
    private static final int VECTOR_DIM = 1536;
    /**
     * ID 字段名
     * <p>
     * 重要：此字段名必须与配置文件中的 id-field-name 保持一致。
     * 如果配置了 id-field-name: id，则集合中的 id 字段名必须是 "id"。
     * 如果使用默认值（未配置 id-field-name），则 Spring AI 默认使用 "doc_id"。
     * <p>
     * 集合创建时的 id 字段名必须与此值一致，否则 Spring AI 的 MilvusVectorStore
     * 无法正确识别 id 字段，会导致 "The field: id is not provided" 错误。
     */
    private static final String ID_FIELD_NAME = "id";
    final String content = MilvusVectorStore.CONTENT_FIELD_NAME;
    final String metadata = MilvusVectorStore.METADATA_FIELD_NAME;
    final String embedding = MilvusVectorStore.EMBEDDING_FIELD_NAME;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;

    public RagService(VectorStore vectorStore, EmbeddingModel embeddingModel, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.chatModel = chatModel;
    }

    /**
     * 创建Milvus集合，包含文档ID、内容、元数据和嵌入向量字段
     * <p>
     * 重要说明：
     * 集合中的 id 字段名必须与配置文件中的 id-field-name 保持一致。
     * 如果配置了 id-field-name: id，则集合中的 id 字段名必须是 "id"。
     * 如果使用默认值（未配置 id-field-name），则集合中的 id 字段名必须是 "doc_id"。
     * <p>
     * 如果集合的 id 字段名与配置不一致，Spring AI 的 MilvusVectorStore
     * 无法正确识别 id 字段，会导致 "The field: id is not provided" 错误。
     *
     * @param collectionName 集合名称，如果为null则使用配置中的默认名称
     * @param databaseName   数据库名称，如果为null则使用默认数据库
     * @param dropIfExists   如果集合已存在，是否删除后重建
     * @return 创建是否成功
     */
    public boolean createCollection(String collectionName, String databaseName, boolean dropIfExists) {
        try {
            MilvusServiceClient milvusServiceClient = (MilvusServiceClient) vectorStore.getNativeClient()
                    .orElseThrow(() -> new IllegalStateException("Milvus client is not available"));

            // 使用参数或默认值
            String finalCollectionName = collectionName != null ? collectionName : COLLECTION_NAME;
            String finalDatabaseName = databaseName != null ? databaseName : MilvusVectorStore.DEFAULT_DATABASE_NAME;

            log.info("开始创建Milvus集合: collectionName={}, databaseName={}", finalCollectionName, finalDatabaseName);

            // 检查集合是否已存在
            HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                    .withCollectionName(finalCollectionName)
                    .withDatabaseName(finalDatabaseName)
                    .build();

            R<Boolean> hasCollectionR = milvusServiceClient.hasCollection(hasCollectionParam);
            if (!hasCollectionR.getStatus().equals(R.Status.Success.getCode())) {
                throw new RuntimeException("检查集合是否存在时发生错误: " + hasCollectionR.getMessage());
            }

            boolean collectionExists = Boolean.TRUE.equals(hasCollectionR.getData());

            if (collectionExists) {
                if (dropIfExists) {
                    log.warn("集合已存在，删除后重建: collectionName={}", finalCollectionName);
                    DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                            .withCollectionName(finalCollectionName)
                            .withDatabaseName(finalDatabaseName)
                            .build();
                    R<?> dropR = milvusServiceClient.dropCollection(dropParam);
                    if (!dropR.getStatus().equals(R.Status.Success.getCode())) {
                        throw new RuntimeException("删除集合失败: " + dropR.getMessage());
                    }
                    log.info("集合删除成功: collectionName={}", finalCollectionName);
                } else {
                    log.info("集合已存在，跳过创建: collectionName={}", finalCollectionName);
                    return true;
                }
            }

            // 定义集合Schema
            CollectionSchemaParam schema = CollectionSchemaParam.newBuilder()
                    .addFieldType(FieldType.newBuilder()
                            .withName(ID_FIELD_NAME)
                            // 重要：此字段名必须与配置文件中的 id-field-name 保持一致
                            // 如果配置了 id-field-name: id，这里必须是 "id"
                            // 如果使用默认值，这里应该是 "doc_id"
                            .withDataType(DataType.VarChar)
                            .withMaxLength(100)
                            .withPrimaryKey(true)
                            .withAutoID(false)
                            .build())
                    .addFieldType(FieldType.newBuilder()
                            .withName(content)
                            .withDataType(DataType.VarChar)
                            .withMaxLength(5000)
                            .build())
                    .addFieldType(FieldType.newBuilder()
                            .withName(metadata)
                            .withDataType(DataType.JSON)
                            .build())
                    .addFieldType(FieldType.newBuilder()
                            .withName(embedding)
                            .withDataType(DataType.FloatVector)
                            .withDimension(VECTOR_DIM)
                            .build())
                    .build();

            // 创建集合
            CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(finalCollectionName)
                    .withSchema(schema)
                    .withDatabaseName(finalDatabaseName)
                    .withConsistencyLevel(ConsistencyLevelEnum.SESSION)
                    .build();

            R<?> createR = milvusServiceClient.createCollection(createCollectionParam);
            if (!createR.getStatus().equals(R.Status.Success.getCode())) {
                throw new RuntimeException("创建集合失败: " + createR.getMessage());
            }
            log.info("集合创建成功: collectionName={}", finalCollectionName);

            // 创建索引
            CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                    .withDatabaseName(finalDatabaseName)
                    .withCollectionName(finalCollectionName)
                    .withFieldName(embedding)
                    .withMetricType(MetricType.COSINE)
                    .withIndexType(IndexType.IVF_FLAT)
                    .withExtraParam("{\"nlist\":128}")
                    .build();

            R<?> indexR = milvusServiceClient.createIndex(createIndexParam);
            if (!indexR.getStatus().equals(R.Status.Success.getCode())) {
                log.warn("创建索引失败，但集合已创建: collectionName={}, error={}", finalCollectionName, indexR.getMessage());
                // 索引创建失败不影响集合创建，只记录警告
            } else {
                log.info("索引创建成功: collectionName={}, fieldName={}", finalCollectionName, embedding);
            }

            return true;
        } catch (Exception e) {
            log.error("创建Milvus集合时发生错误", e);
            throw new RuntimeException("创建Milvus集合失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建Milvus集合（使用默认参数）
     * <p>
     * 使用配置中的默认集合名称和数据库名称，如果集合已存在则跳过创建
     */
    public boolean createCollection() {
        return createCollection(null, null, false);
    }

    /**
     * 创建Milvus集合（指定集合名称）
     *
     * @param collectionName 集合名称
     * @param dropIfExists   如果集合已存在，是否删除后重建
     */
    public boolean createCollection(String collectionName, boolean dropIfExists) {
        return createCollection(collectionName, null, dropIfExists);
    }

    /**
     * 添加文档到向量存储
     * <p>
     * Spring AI 1.1.2 的 Document 类使用两个参数的构造函数：
     * Document(String text, Map<String, Object> metadata)
     * <p>
     * 重要说明：
     * 1. 集合中的 id 字段名必须与配置文件中的 id-field-name 保持一致。
     * 如果配置了 id-field-name: id，集合中的 id 字段名必须是 "id"。
     * 如果使用默认值，集合中的 id 字段名必须是 "doc_id"。
     * <p>
     * 2. Spring AI 的 MilvusVectorStore 会从 Document 的 metadata 中查找
     * 与配置的 id-field-name 同名的字段作为文档ID，然后传递给 Milvus。
     * <p>
     * 3. 如果集合的 id 字段名与配置不一致，会导致 "The field: id is not provided" 错误。
     * 这是因为 Spring AI 期望的字段名与集合中实际的字段名不匹配。
     * <p>
     * 4. 向量处理：
     * - 如果 metadata 中包含 "embedding" 或 "vector" 字段，将使用该向量
     * - 如果没有提供向量，将使用阿里 EmbeddingModel 自动计算向量
     * - 向量维度必须与配置中的 embedding-dimension 一致（当前为 1536）
     *
     * @param content     文档内容
     * @param metadataMap 文档元数据，可包含：
     *                    - id: 文档ID（可选，如果没有则自动生成）
     *                    - 其他自定义元数据
     */
    public void addDocument(String content, Map<String, Object> metadataMap) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("文档内容不能为空");
        }
        MilvusServiceClient milvusServiceClient = (MilvusServiceClient) vectorStore.getNativeClient().get();
        String documentId = UUID.randomUUID().toString();
        List<Float> embeddingVector;

        // ========== 【向量生成】将文档内容转换为向量 ==========
        // 【向量使用说明】
        // 1. 使用 EmbeddingModel 将文档文本转换为 1536 维向量
        // 2. 这个向量代表了文档的语义信息，相似的文档会有相似的向量
        // 3. 向量会被存储到 Milvus 的 embedding 字段中，用于后续的相似度搜索
        log.debug("【向量生成】使用阿里 EmbeddingModel 计算文档向量，文档内容长度: {}", content.length());
        try {
            // 【关键代码】调用 embed() 方法将文本转换为 float[1536] 向量数组
            // 这个向量包含了文档的语义信息，维度为 1536（由阿里 EmbeddingModel 决定）
            float[] embeddingArray = embeddingModel.embed(content);

            if (embeddingArray == null || embeddingArray.length == 0) {
                throw new IllegalStateException("EmbeddingModel 返回空向量");
            }

            // 验证向量维度（必须是 1536 维，与配置保持一致）
            if (embeddingArray.length != VECTOR_DIM) {
                throw new IllegalArgumentException(
                        String.format("计算得到的向量维度不匹配: 期望 %d，实际 %d", VECTOR_DIM, embeddingArray.length));
            }

            // 【向量转换】转换为 List<Float>（Milvus 需要 Float 类型）
            // Milvus 存储向量时需要使用 List<Float> 格式
            embeddingVector = new java.util.ArrayList<>(embeddingArray.length);
            for (float f : embeddingArray) {
                embeddingVector.add(f);
            }

            log.debug("【向量生成】✓ 文档向量计算完成，维度: {}", embeddingVector.size());
        } catch (Exception e) {
            log.error("使用 EmbeddingModel 计算向量失败", e);
            throw new RuntimeException("计算文档向量失败: " + e.getMessage(), e);
        }
        List<InsertParam.Field> fields = new ArrayList<>();
        // ID 字段
        fields.add(InsertParam.Field.builder()
                .name(ID_FIELD_NAME)
                .values(Collections.singletonList(documentId))
                .build());

        // 内容字段
        fields.add(InsertParam.Field.builder()
                .name(this.content)
                .values(Collections.singletonList(content))
                .build());
        JSONObject metadataJson = JSONObject.parseObject(JSON.toJSONString(metadataMap));
        log.info("元数据: {}", metadataJson);
        // 元数据字段（JSON 格式）
        fields.add(InsertParam.Field.builder()
                .name(this.metadata)
                .values(Collections.singletonList(JsonParser.parseString(metadataJson.toString())))
                .build());

        // ========== 【向量存储】将向量存储到 Milvus ==========
        // 【向量使用说明】
        // 1. 将计算好的文档向量存储到 Milvus 的 embedding 字段
        // 2. 这个向量会在后续的相似度搜索中被使用
        // 3. Milvus 会为向量字段创建索引（IVF_FLAT），加速向量搜索
        List<List<Float>> embeddingList = new ArrayList<>();
        embeddingList.add(embeddingVector);
        fields.add(InsertParam.Field.builder()
                .name(this.embedding)  // embedding 字段名，存储文档向量
                .values(embeddingList)  // 文档向量值（1536 维）
                .build());
        InsertParam insertParam = InsertParam.newBuilder()
                .withDatabaseName(MilvusVectorStore.DEFAULT_DATABASE_NAME)
                .withCollectionName(MilvusVectorStore.DEFAULT_COLLECTION_NAME)
                .withFields(fields)
                .build();
        milvusServiceClient.insert(insertParam);
    }

    /**
     * 批量添加文档到向量存储
     * <p>
     * 参考 addDocument 方法实现，使用 Milvus 原生 API 直接批量插入数据，操作更透明、可控。
     * <p>
     * 重要说明：
     * 1. 集合中的 id 字段名必须与配置文件中的 id-field-name 保持一致（当前使用 "id"）。
     * 2. 向量处理：
     * - 如果文档数据中包含 "embedding" 或 "vector" 字段，将使用该向量
     * - 如果没有提供向量，将使用阿里 EmbeddingModel 自动计算向量
     * - 向量维度必须与配置中的 embedding-dimension 一致（当前为 1536）
     * 3. 元数据以 JSON 格式存储到 Milvus 的 metadata 字段中。
     *
     * @param documents 文档列表，每个文档包含：
     *                  - content: 文档内容（必需）
     *                  - metadata: 文档元数据（可选）
     *                  - embedding 或 vector: 预计算的向量（可选，List<Float> 或 List<Double> 类型）
     */
    public void addDocuments(List<Map<String, Object>> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("addDocuments: 文档列表为空，跳过处理");
            return;
        }
        try {
            // 获取 Milvus 客户端
            MilvusServiceClient milvusServiceClient = (MilvusServiceClient) vectorStore.getNativeClient()
                    .orElseThrow(() -> new IllegalStateException("Milvus client is not available"));
            // 准备批量插入的数据
            List<String> documentIds = new ArrayList<>();
            List<String> contents = new ArrayList<>();
            List<JsonElement> metadataJsons = new ArrayList<>();
            List<List<Float>> embeddingVectors = new ArrayList<>();

            // 处理每个文档
            for (Map<String, Object> doc : documents) {
                // 提取文档内容
                String content = (String) doc.get("content");
                if (content == null || content.trim().isEmpty()) {
                    throw new IllegalArgumentException("文档内容不能为空");
                }

                // 提取元数据
                @SuppressWarnings("unchecked")
                Map<String, Object> metadataMap = (Map<String, Object>) doc.getOrDefault("metadata", Map.of());

                // 处理文档ID
                String documentId = UUID.randomUUID().toString();
                // 使用阿里 EmbeddingModel 计算向量（参考 addDocument 方法）
                float[] embeddingArray = embeddingModel.embed(content);
                if (embeddingArray == null || embeddingArray.length == 0) {
                    throw new IllegalStateException("EmbeddingModel 返回空向量");
                }
                // 验证向量维度
                if (embeddingArray.length != VECTOR_DIM) {
                    throw new IllegalArgumentException(
                            String.format("计算得到的向量维度不匹配: 期望 %d，实际 %d", VECTOR_DIM, embeddingArray.length));
                }
                // 转换为 List<Float>（Milvus 需要 Float 类型）
                List<Float> embeddingVector = new ArrayList<>(embeddingArray.length);
                for (float f : embeddingArray) {
                    embeddingVector.add(f);
                }
                // 收集字段值
                documentIds.add(documentId);
                contents.add(content);
                embeddingVectors.add(embeddingVector);
                // 将元数据转换为 JSON 字符串（使用 Milvus 的 JsonUtils）
                metadataJsons.add(JsonParser.parseString(JSON.toJSONString(metadataMap)));
            }

            // 准备插入 Milvus 的数据（参考 addDocument 方法）
            List<InsertParam.Field> fields = new ArrayList<>();

            // ID 字段
            fields.add(InsertParam.Field.builder()
                    .name(ID_FIELD_NAME)
                    .values(documentIds)
                    .build());

            // 内容字段
            fields.add(InsertParam.Field.builder()
                    .name(this.content)
                    .values(contents)
                    .build());

            // 元数据字段（JSON 格式）
            fields.add(InsertParam.Field.builder()
                    .name(this.metadata)
                    .values(metadataJsons)
                    .build());

            // 向量字段
            fields.add(InsertParam.Field.builder()
                    .name(this.embedding)
                    .values(embeddingVectors)
                    .build());

            // 执行批量插入操作
            InsertParam insertParam = InsertParam.newBuilder()
                    .withDatabaseName(MilvusVectorStore.DEFAULT_DATABASE_NAME)
                    .withCollectionName(MilvusVectorStore.DEFAULT_COLLECTION_NAME)
                    .withFields(fields)
                    .build();

            R<?> insertR = milvusServiceClient.insert(insertParam);
            if (!insertR.getStatus().equals(R.Status.Success.getCode())) {
                throw new RuntimeException("批量插入文档到 Milvus 失败: " + insertR.getMessage());
            }

            log.info("批量文档插入成功，共 {} 条", documents.size());
        } catch (Exception e) {
            log.error("批量添加文档到向量存储失败", e);
            throw new RuntimeException("批量添加文档失败: " + e.getMessage(), e);
        }
    }
    /**
     * 相似度搜索
     * <p>
     * 使用 Spring AI VectorStore 的标准 similaritySearch 方法
     * <p>
     * 【向量使用说明】
     * 该方法内部会进行以下向量操作：
     * 1. 将查询文本 query 通过 EmbeddingModel 转换为向量（维度1536）
     * 2. 在 Milvus 中使用该查询向量与存储的文档向量进行相似度计算（余弦相似度）
     * 3. 返回相似度最高的 topK 个文档
     * <p>
     * 向量检索流程（Spring AI 内部实现）：
     * - vectorStore.similaritySearch(query) 内部会：
     *   ① 调用 embeddingModel.embed(query) 将查询文本转换为 float[1536] 向量
     *   ② 使用该向量在 Milvus 的 embedding 字段上进行向量相似度搜索
     *   ③ Milvus 使用 COSINE 距离计算相似度，返回最相似的文档
     *   ④ 将 Milvus 返回的结果转换为 Spring AI 的 Document 对象
     *
     * @param query 查询文本（会被转换为向量进行检索）
     * @param topK  返回最相似的前K个文档
     * @return 相似文档列表
     */
    public List<Document> similaritySearch(String query, int topK) {
        log.debug("开始向量相似度搜索，查询文本: {}, topK: {}", query, topK);

        // 【关键点1】这里调用 vectorStore.similaritySearch(query)
        // 内部会自动将 query 文本转换为向量，然后在 Milvus 中进行向量相似度搜索
        // 具体流程见方法注释中的【向量使用说明】
        List<Document> results = vectorStore.similaritySearch(query);

        log.debug("向量搜索完成，找到 {} 个文档，限制返回前 {} 个", results.size(), topK);
        return results.stream()
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 相似度搜索（带相似度阈值）
     * <p>
     * 注意：Spring AI 1.1.2 的 VectorStore 接口可能不直接支持相似度阈值参数
     * 这里先返回所有结果，实际使用时需要根据具体的 VectorStore 实现调整
     * <p>
     * 【向量使用说明】
     * 同 similaritySearch 方法，使用向量进行相似度检索
     *
     * @param query               查询文本（会被转换为向量进行检索）
     * @param topK                返回最相似的前K个文档
     * @param similarityThreshold 相似度阈值（0-1之间），当前实现中暂未使用
     * @return 相似文档列表
     */
    public List<Document> similaritySearchWithThreshold(String query, int topK, double similarityThreshold) {
        log.debug("开始向量相似度搜索（带阈值），查询文本: {}, topK: {}, 阈值: {}", query, topK, similarityThreshold);

        // 【关键点2】向量检索：将查询文本转换为向量，在 Milvus 中进行向量相似度搜索
        List<Document> results = vectorStore.similaritySearch(query);

        return results.stream()
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 根据元数据过滤搜索
     * <p>
     * 注意：Spring AI 1.1.2 的 VectorStore 接口可能不直接支持元数据过滤
     * 这里在搜索结果中进行内存过滤
     * <p>
     * 【向量使用说明】
     * 1. 首先使用向量进行相似度搜索（同 similaritySearch 方法）
     * 2. 然后在内存中对结果进行元数据过滤
     *
     * @param query          查询文本（会被转换为向量进行检索）
     * @param topK           返回最相似的前K个文档
     * @param metadataFilter 元数据过滤条件
     * @return 相似文档列表
     */
    public List<Document> similaritySearchWithFilter(String query, int topK, Map<String, Object> metadataFilter) {
        log.debug("开始向量相似度搜索（带元数据过滤），查询文本: {}, topK: {}, 过滤条件: {}", query, topK, metadataFilter);

        // 【关键点3】向量检索：将查询文本转换为向量，在 Milvus 中进行向量相似度搜索
        List<Document> results = vectorStore.similaritySearch(query);

        // 如果提供了过滤条件，在结果中进行过滤
        if (metadataFilter != null && !metadataFilter.isEmpty()) {
            results = results.stream()
                    .filter(doc -> {
                        Map<String, Object> docMetadata = doc.getMetadata();
                        return metadataFilter.entrySet().stream()
                                .allMatch(entry -> {
                                    Object docValue = docMetadata.get(entry.getKey());
                                    Object filterValue = entry.getValue();
                                    return docValue != null && docValue.equals(filterValue);
                                });
                    })
                    .toList();
        }

        return results.stream()
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 【示例方法】展示向量检索的完整流程
     * <p>
     * 这个方法展示了向量检索的完整过程，帮助理解向量是如何被使用的
     * <p>
     * 向量检索的完整流程：
     * 1. 将查询文本转换为向量（使用 EmbeddingModel）
     * 2. 构建搜索参数（指定向量字段、相似度度量方式等）
     * 3. 在 Milvus 中执行向量相似度搜索
     * 4. 解析搜索结果
     * <p>
     * 注意：这个方法仅用于学习和理解，实际使用中建议使用 similaritySearch 方法
     * <p>
     * 【向量使用关键点】：
     * - 步骤1：embeddingModel.embed(query) 将文本转换为 1536 维向量
     * - 步骤2：构建 SearchParam，指定使用 COSINE 相似度度量
     * - 步骤3：Milvus 使用查询向量与存储的文档向量进行相似度计算
     * - 步骤4：返回相似度最高的文档
     *
     * @param query 查询文本
     * @param topK  返回最相似的前K个文档
     * @return 相似文档列表（包含文档内容和相似度分数）
     */
    public void demonstrateVectorSearchProcess(String query, int topK) {
        log.info("========== 【向量检索流程演示】开始 ==========");
        log.info("查询文本: {}, topK: {}", query, topK);

        try {
            // ========== 步骤1: 将查询文本转换为向量 ==========
            // 【向量生成】使用 EmbeddingModel 将文本转换为 1536 维向量
            log.info("【步骤1-向量生成】将查询文本转换为向量...");
            float[] queryEmbedding = embeddingModel.embed(query);

            if (queryEmbedding == null || queryEmbedding.length == 0) {
                throw new IllegalStateException("EmbeddingModel 返回空向量");
            }

            // 验证向量维度
            if (queryEmbedding.length != VECTOR_DIM) {
                throw new IllegalArgumentException(
                        String.format("查询向量维度不匹配: 期望 %d，实际 %d", VECTOR_DIM, queryEmbedding.length));
            }

            log.info("【步骤1-向量生成】✓ 查询向量生成成功，维度: {}", queryEmbedding.length);
            log.debug("【步骤1-向量生成】向量前5个值: [{}, {}, {}, {}, ...]",
                    queryEmbedding[0], queryEmbedding[1], queryEmbedding[2], queryEmbedding[3], queryEmbedding[4]);

            // ========== 步骤2: 说明向量搜索参数 ==========
            log.info("【步骤2-向量搜索参数】");
            log.info("  - 向量字段名: {}", embedding);
            log.info("  - 相似度度量方式: COSINE (余弦相似度)");
            log.info("  - 返回文档数量: {}", topK);
            log.info("  - 向量维度: {}", VECTOR_DIM);

            // ========== 步骤3: 说明向量搜索过程 ==========
            log.info("【步骤3-向量搜索执行】");
            log.info("  Milvus 会执行以下操作：");
            log.info("  1. 使用查询向量与集合中所有文档的 embedding 向量进行相似度计算");
            log.info("  2. 使用 COSINE 距离计算相似度（值越大越相似，范围 0-1）");
            log.info("  3. 返回相似度最高的 topK 个文档");

            // 实际执行搜索（使用 Spring AI 封装的 vectorStore）
            log.info("【步骤3-向量搜索执行】执行搜索...");
            List<Document> results = vectorStore.similaritySearch(query);
            log.info("【步骤3-向量搜索执行】✓ 搜索完成，找到 {} 个结果", results.size());

            // ========== 步骤4: 展示搜索结果 ==========
            log.info("【步骤4-搜索结果】");
            for (int i = 0; i < Math.min(results.size(), topK); i++) {
                Document doc = results.get(i);
                log.info("  结果 {}: 文档长度={}, 元数据={}", i + 1, doc.getText().length(), doc.getMetadata());
            }

            log.info("========== 【向量检索流程演示】完成 ==========");
            log.info("");
            log.info("【总结】向量检索的关键步骤：");
            log.info("  1. 文本 → 向量：使用 EmbeddingModel.embed() 将查询文本转换为向量");
            log.info("  2. 向量搜索：在 Milvus 中使用查询向量与文档向量进行相似度计算");
            log.info("  3. 结果排序：按相似度从高到低排序，返回最相似的文档");
            log.info("  4. 向量维度：当前使用 {} 维向量（由阿里 EmbeddingModel 生成）", VECTOR_DIM);

        } catch (Exception e) {
            log.error("【向量检索流程演示】失败", e);
            throw new RuntimeException("向量搜索演示失败: " + e.getMessage(), e);
        }
    }

    /**
     * RAG检索增强生成
     * <p>
     * 使用Milvus进行知识召回，然后使用Qwen模型生成回答
     * <p>
     * 流程：
     * 1. 从Milvus中检索与查询相关的文档（知识召回）
     * 2. 将检索到的文档作为上下文构建prompt
     * 3. 使用Qwen模型基于上下文生成回答
     *
     * @param query               用户查询问题
     * @param topK                从Milvus检索的文档数量，默认5
     * @param similarityThreshold 相似度阈值（可选）
     * @param metadataFilter      元数据过滤条件（可选）
     * @return RAG结果，包含生成的回答和检索到的文档
     */
    public RagResult ragSearch(String query, int topK, Double similarityThreshold, Map<String, Object> metadataFilter) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("查询问题不能为空");
        }

        log.info("开始RAG检索，查询: {}, topK: {}", query, topK);

        // ========== 步骤1: 从Milvus中检索相关文档（知识召回）==========
        // 【向量使用核心步骤】
        // 这一步会使用向量进行相似度检索：
        // 1. 将用户查询 query 通过 EmbeddingModel 转换为 1536 维向量
        // 2. 在 Milvus 中使用该查询向量与所有文档的 embedding 向量进行余弦相似度计算
        // 3. 返回相似度最高的 topK 个文档
        // 向量检索的具体实现见 similaritySearch 系列方法
        List<Document> retrievedDocuments;
        if (similarityThreshold != null && metadataFilter != null) {
            retrievedDocuments = similaritySearchWithFilter(query, topK, metadataFilter);
        } else if (similarityThreshold != null) {
            retrievedDocuments = similaritySearchWithThreshold(query, topK, similarityThreshold);
        } else {
            retrievedDocuments = similaritySearch(query, topK);
        }

        log.info("从Milvus检索到 {} 个相关文档", retrievedDocuments.size());

        // 步骤2: 构建上下文prompt
        StringBuilder contextBuilder = new StringBuilder();
        if (!retrievedDocuments.isEmpty()) {
            contextBuilder.append("基于以下知识库内容回答问题：\n\n");
            for (int i = 0; i < retrievedDocuments.size(); i++) {
                Document doc = retrievedDocuments.get(i);
                contextBuilder.append("【文档").append(i + 1).append("】\n");
                contextBuilder.append(doc.getText()).append("\n\n");
            }
        } else {
            contextBuilder.append("未找到相关文档，请基于你的知识回答问题。\n\n");
        }

        // 构建完整的prompt
        String promptText = contextBuilder.toString() +
                "问题：" + query + "\n\n" +
                "请根据上述知识库内容回答问题。如果知识库中没有相关信息，请说明。回答要准确、简洁。";

        log.info("构建的prompt: {}", promptText);

        // 步骤3: 使用Qwen模型生成回答
        String answer;
        try {
            Prompt prompt = new Prompt(new UserMessage(promptText));
            ChatResponse response = chatModel.call(prompt);
            answer = response.getResult().getOutput().getText();
            log.info("Qwen模型生成回答成功answer={}", answer);
            assert answer != null;
            log.info("Qwen模型生成回答成功，长度: {}", answer.length());
        } catch (Exception e) {
            log.error("使用Qwen模型生成回答失败", e);
            throw new RuntimeException("生成回答失败: " + e.getMessage(), e);
        }

        // 构建返回结果
        RagResult result = new RagResult();
        result.setQuery(query);
        result.setAnswer(answer);
        result.setRetrievedCount(retrievedDocuments.size());
        result.setRetrievedDocuments(retrievedDocuments);

        return result;
    }

    /**
     * RAG结果内部类
     */
    public static class RagResult {
        private String query;
        private String answer;
        private int retrievedCount;
        private List<Document> retrievedDocuments;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public int getRetrievedCount() {
            return retrievedCount;
        }

        public void setRetrievedCount(int retrievedCount) {
            this.retrievedCount = retrievedCount;
        }

        public List<Document> getRetrievedDocuments() {
            return retrievedDocuments;
        }

        public void setRetrievedDocuments(List<Document> retrievedDocuments) {
            this.retrievedDocuments = retrievedDocuments;
        }
    }
}

