package com.example.websocketchatbacked.repository.es;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.example.websocketchatbacked.entity.EsKbChunk;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.client.RequestOptions;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class EsChunkRepository {

    private final RestHighLevelClient restHighLevelClient;

    private static final String INDEX_NAME = "kb_document_chunk";

    /**
     * 批量保存分块到ES
     */
    public void bulkSave(List<EsKbChunk> esChunkList) throws IOException {
        if (esChunkList.isEmpty()) {
            return;
        }

        BulkRequest bulkRequest = new BulkRequest(INDEX_NAME);

        for (EsKbChunk esChunk : esChunkList) {
            IndexRequest request = new IndexRequest(INDEX_NAME)
                    .id(esChunk.getChunkId()) // 用chunkId作为ES文档ID
                    .source(esChunk, XContentType.JSON);
            bulkRequest.add(request);
        }

        // 执行批量请求
        BulkResponse response = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);

        // 可选：处理失败的批次
        if (response.hasFailures()) {
            throw new RuntimeException("ES批量写入失败: " + response.buildFailureMessage());
        }
    }
}