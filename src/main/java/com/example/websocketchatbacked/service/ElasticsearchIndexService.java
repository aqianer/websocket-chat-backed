package com.example.websocketchatbacked.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import com.example.websocketchatbacked.model.FileChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ElasticsearchIndexService {
    
    @Autowired
    private ElasticsearchClient elasticsearchClient;
    
    private static final String INDEX_NAME = "kb_chunks";
    private static final int BATCH_SIZE = 100;
    
    public void ensureIndexExists() {
        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (!exists) {
                createIndex();
            }
        } catch (IOException e) {
            log.error("检查索引存在性失败", e);
        }
    }
    
    private void createIndex() throws IOException {
        CreateIndexRequest request = CreateIndexRequest.of(c -> c
                .index(INDEX_NAME)
                .mappings(m -> m
                        .properties("docId", p -> p.long_(l -> l))
                        .properties("kbId", p -> p.long_(l -> l))
                        .properties("content", p -> p.text(t -> t))
                        .properties("chunkNum", p -> p.integer(i -> i))
                        .properties("metadata", p -> p.text(t -> t))
                        .properties("createTime", p -> p.date(d -> d))
                )
        );
        
        CreateIndexResponse response = elasticsearchClient.indices().create(request);
        log.info("创建Elasticsearch索引成功: index={}, acknowledged={}", INDEX_NAME, response.acknowledged());
    }
    
    public void batchIndex(List<FileChunk> chunks) throws IOException {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        
        ensureIndexExists();
        
        List<BulkOperation> operations = new ArrayList<>();
        for (FileChunk chunk : chunks) {
            Map<String, Object> document = new HashMap<>();
            document.put("docId", chunk.getDocId());
            document.put("kbId", chunk.getKbId());
            document.put("content", chunk.getContent());
            document.put("chunkNum", chunk.getChunkNum());
            document.put("metadata", chunk.getMetadata());
            document.put("createTime", System.currentTimeMillis());
            
            IndexOperation<Map<String, Object>> indexOp = IndexOperation.of(i -> i
                    .index(INDEX_NAME)
                    .document(document)
            );
            
            operations.add(BulkOperation.of(b -> b.index(indexOp)));
            
            if (operations.size() >= BATCH_SIZE) {
                executeBulk(operations);
                operations.clear();
            }
        }
        
        if (!operations.isEmpty()) {
            executeBulk(operations);
        }
    }
    
    private void executeBulk(List<BulkOperation> operations) throws IOException {
        BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
        BulkResponse response = elasticsearchClient.bulk(bulkRequest);
        
        if (response.errors()) {
            log.error("批量索引存在错误");
            response.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("索引错误: id={}, error={}", item.id(), item.error().reason());
                }
            });
        } else {
            log.info("批量索引成功: count={}", response.items().size());
        }
    }
    
    public void deleteByDocId(Long docId) throws IOException {
        Query query = Query.of(q -> q
                .term(t -> t
                        .field("docId")
                        .value(docId)
                )
        );
        
        elasticsearchClient.deleteByQuery(d -> d
                .index(INDEX_NAME)
                .query(query)
        );
        
        log.info("删除文档ES索引成功: docId={}", docId);
    }
    
    public void deleteByKbId(Long kbId) throws IOException {
        Query query = Query.of(q -> q
                .term(t -> t
                        .field("kbId")
                        .value(kbId)
                )
        );
        
        elasticsearchClient.deleteByQuery(d -> d
                .index(INDEX_NAME)
                .query(query)
        );
        
        log.info("删除知识库ES索引成功: kbId={}", kbId);
    }
}