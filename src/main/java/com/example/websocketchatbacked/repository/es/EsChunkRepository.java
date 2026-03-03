package com.example.websocketchatbacked.repository.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import com.example.websocketchatbacked.entity.EsKbChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

/**
 * ES分块仓储层
 * 适配 Elasticsearch 8.x 官方客户端（ElasticsearchClient）
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EsChunkRepository {

    // 注入你EsConfig中配置的ElasticsearchClient
    private final ElasticsearchClient esClient;

    // ES索引名（和你定义的kb_document_chunk索引对应）
    private static final String INDEX_NAME = "kb_document_chunk";

    /**
     * 批量保存分块到ES
     * @param esChunkList 待写入的ES分块列表
     * @throws IOException ES客户端异常
     */
    public void bulkSave(List<EsKbChunk> esChunkList) throws IOException {
        // 空列表直接返回，避免无效请求
        if (esChunkList.isEmpty()) {
            log.warn("ES批量写入：分块列表为空，无需执行写入");
            return;
        }

        // 构建批量请求
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (EsKbChunk esChunk : esChunkList) {
            // 构建单条索引操作：用chunkId作为ES文档ID，保证幂等性
            IndexOperation<EsKbChunk> indexOp = new IndexOperation.Builder<EsKbChunk>()
                    .index(INDEX_NAME)       // 指定索引名
                    .id(esChunk.getChunkId()) // 用chunk_id作为文档ID
                    .document(esChunk)        // 要写入的文档内容
                    .build();

            // 将单条操作添加到批量请求
            bulkBuilder.operations(BulkOperation.of(op -> op.index(indexOp)));
        }

        // 执行批量写入
        BulkResponse response = esClient.bulk(bulkBuilder.build());

        // 处理批量写入结果
        if (response.errors()) {
            log.error("ES批量写入失败，错误信息：{}", response.errors());
            // 遍历失败的操作，打印详细错误
            response.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("分块ID:{} 写入失败，原因：{}", item.id(), item.error().reason());
                }
            });
            throw new RuntimeException("ES批量写入分块失败，部分或全部文档写入异常");
        }

        log.info("ES批量写入成功，共写入{}条分块数据", esChunkList.size());
    }
}