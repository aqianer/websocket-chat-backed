package com.milvus.config;

import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "vector.milvus")
public class MilvusConfig {

    private static final Logger log = LoggerFactory.getLogger(MilvusConfig.class);

    private String host = "localhost";
    private Integer port = 19530;
    private String database = "default";
    private Boolean autoCreateCollection = true;
    private String username;
    private String password;
    private List<CollectionConfig> collections;

    private MilvusClient milvusClient;

    @Data
    public static class CollectionConfig {
        private int dimension;
        private String name;
        private String indexType;
        private List<String> metadataFields;
    }

    @Bean
    public MilvusServiceClient milvusServiceClient() {
        ConnectParam.Builder connectBuilder = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(database);
        
        if (username != null && !username.isEmpty()) {
            connectBuilder.withAuthorization(username, password);
        }
        
        return new MilvusServiceClient(connectBuilder.build());
    }

    @Bean
    public MilvusEmbeddingStore milvusEmbeddingStore() {
        MilvusEmbeddingStore.Builder builder = MilvusEmbeddingStore.builder()
                .host(host)
                .port(port)
                .databaseName(database);
        
        if (username != null && !username.isEmpty()) {
            builder.username(username).password(password);
        }
        
        return builder.build();
    }

    @PostConstruct
    public void initCollections() {
        if (collections == null || !autoCreateCollection) {
            log.info("Auto-create collection is disabled or no collections configured");
            return;
        }

        milvusClient = milvusServiceClient();
        
        for (CollectionConfig config : collections) {
            try {
                createCollectionIfNotExists(config);
            } catch (Exception e) {
                log.error("Failed to create collection: {}, error: {}", config.getName(), e.getMessage(), e);
            }
        }
    }

    private void createCollectionIfNotExists(CollectionConfig config) {
        R<Boolean> hasCollection = milvusClient.hasCollection(HasCollectionParam.newBuilder()
                .withCollectionName(config.getName())
                .build());
        
        if (hasCollection.getData()) {
            log.info("Collection already exists: {}, skipping creation", config.getName());
            return;
        }

        log.info("Creating collection: {}", config.getName());

        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();
        
        FieldType vectorField = FieldType.newBuilder()
                .withName("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(config.getDimension())
                .build();
        
        List<FieldType> fields = new ArrayList<>();
        fields.add(idField);
        fields.add(vectorField);
        
        for (String metaField : config.getMetadataFields()) {
            fields.add(FieldType.newBuilder()
                    .withName(metaField)
                    .withDataType(DataType.VarChar)
                    .withMaxLength(512)
                    .build());
        }
        
        R<RpcStatus> createCollection = milvusClient.createCollection(CreateCollectionParam.newBuilder()
                .withCollectionName(config.getName())
                .withFieldTypes(fields)
                .withConsistencyLevel(ConsistencyLevelEnum.BOUNDED)
                .build());
        
        if (createCollection.getStatus() == R.Status.Success.getCode()) {
            log.info("Collection created successfully: {}", config.getName());
            
            milvusClient.createIndex(CreateIndexParam.newBuilder()
                    .withCollectionName(config.getName())
                    .withFieldName("vector")
                    .withIndexType(IndexType.valueOf(config.getIndexType()))
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"nlist\":1024}")
                    .build());
            
            milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(config.getName())
                    .build());
            
            log.info("Index created and collection loaded: {}", config.getName());
        } else {
            log.error("Failed to create collection: {}", config.getName());
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public Boolean getAutoCreateCollection() {
        return autoCreateCollection;
    }

    public void setAutoCreateCollection(Boolean autoCreateCollection) {
        this.autoCreateCollection = autoCreateCollection;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<CollectionConfig> getCollections() {
        return collections;
    }

    public void setCollections(List<CollectionConfig> collections) {
        this.collections = collections;
    }
}