package com.example.websocketchatbacked.controller.ws;

import com.example.websocketchatbacked.service.impl.AsyncTaskService;
import com.example.websocketchatbacked.util.SpringContextUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/ws/fileParse/{userId}")
public class FileParseEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(FileParseEndpoint.class);

    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private String userId;

    private static ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private AsyncTaskService getAsyncTaskService() {
        return SpringContextUtil.getBean(AsyncTaskService.class);
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.userId = userId;
        sessionMap.put(userId, session);
        logger.info("用户 {} 建立WebSocket连接，当前连接数：{}", userId, sessionMap.size());
        try {
            sendConnectionEstablishedMessage();
        } catch (IOException e) {
            logger.error("用户 {} 发送连接建立消息失败：{}", userId, e.getMessage(), e);
        }
    }

    @OnClose
    public void onClose() {
        sessionMap.remove(userId);
        logger.info("用户 {} 关闭WebSocket连接，当前连接数：{}", userId, sessionMap.size());
    }

    @OnMessage
    public void onMessage(String message) {
        logger.info("收到用户 {} 的消息：{}", userId, message);

        try {
            FileParseRequestWrapper wrapper = objectMapper.readValue(message, FileParseRequestWrapper.class);
            
            if (!"parse".equals(wrapper.getType())) {
                logger.warn("用户 {} 发送了不支持的消息类型：{}", userId, wrapper.getType());
                sendErrorMessage("不支持的消息类型");
                return;
            }

            FileParseRequestData data = wrapper.getData();
            
            logger.info("解析请求参数 - kbId: {}, documentIds: {}, parseStrategy: {}, extractContent: {}, segmentStrategy: {}",
                    data.getKbId(), data.getDocumentIds(), data.getParseStrategy(),
                    data.getExtractContent(), data.getSegmentStrategy());

            if (data.getKbId() == null) {
                logger.warn("用户 {} 的请求缺少kbId参数", userId);
                sendErrorMessage("知识库ID不能为空");
                return;
            }

            if (data.getDocumentIds() == null || data.getDocumentIds().isEmpty()) {
                logger.warn("用户 {} 的请求缺少documentIds参数", userId);
                sendErrorMessage("文档ID列表不能为空");
                return;
            }

            if (data.getParseStrategy() == null) {
                logger.warn("用户 {} 的请求缺少parseStrategy参数", userId);
                sendErrorMessage("解析策略不能为空");
                return;
            }

            if (data.getSegmentStrategy() == null) {
                logger.warn("用户 {} 的请求缺少segmentStrategy参数", userId);
                sendErrorMessage("分段策略不能为空");
                return;
            }

            sendProgressMessage("开始处理文档，共 {} 个", data.getDocumentIds().size());

            int totalDocuments = data.getDocumentIds().size();
            int processedCount = 0;

            for (Long documentId : data.getDocumentIds()) {
                logger.info("开始处理文档 - documentId: {}, kbId: {}", documentId, data.getKbId());
                
                try {
                    sendDocumentProgressMessage(documentId, "开始解析", processedCount, totalDocuments);
                    
                    String extractContentStr = data.getExtractContent() != null ? 
                            String.join(",", data.getExtractContent()) : "";
                    
                    getAsyncTaskService().processChunk(documentId, data.getParseStrategy(), data.getSegmentStrategy());
                    
                    processedCount++;
                    sendDocumentProgressMessage(documentId, "解析完成", processedCount, totalDocuments);
                    
                    logger.info("文档处理完成 - documentId: {}, 进度: {}/{}", documentId, processedCount, totalDocuments);
                    
                } catch (Exception e) {
                    logger.error("文档处理失败 - documentId: {}, 错误: {}", documentId, e.getMessage(), e);
                    sendDocumentErrorMessage(documentId, "处理失败: " + e.getMessage());
                }
            }

            sendCompletionMessage(totalDocuments);
            logger.info("所有文档处理完成 - 用户: {}, 总数: {}", userId, totalDocuments);

        } catch (Exception e) {
            logger.error("处理用户 {} 的消息时发生错误：{}", userId, e.getMessage(), e);
            try {
                sendErrorMessage("消息处理失败: " + e.getMessage());
            } catch (IOException ex) {
                logger.error("给用户 {} 发送错误消息失败：{}", userId, ex.getMessage(), ex);
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("用户 {} WebSocket连接错误：{}", userId, error.getMessage(), error);
    }

    public static void sendMessageToUser(String userId, String message) {
        Session session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                logger.info("给用户 {} 发送消息成功：{}", userId, message);
            } catch (IOException e) {
                logger.error("给用户 {} 发送消息失败：{}", userId, e.getMessage(), e);
            }
        } else {
            logger.warn("用户 {} 的WebSocket连接已关闭或不存在", userId);
        }
    }

    private void sendConnectionEstablishedMessage() throws IOException {
        WebSocketResponse response = new WebSocketResponse();
        response.setType("connection_established");
        response.setMessage("WebSocket连接已建立");
        response.setData(Map.of("userId", userId, "timestamp", System.currentTimeMillis()));
        sendMessage(objectMapper.writeValueAsString(response));
    }

    private void sendProgressMessage(String format, Object... args) throws IOException {
        WebSocketResponse response = new WebSocketResponse();
        response.setType("progress");
        response.setMessage(String.format(format, args));
        sendMessage(objectMapper.writeValueAsString(response));
    }

    private void sendDocumentProgressMessage(Long documentId, String status, int processed, int total) throws IOException {
        WebSocketResponse response = new WebSocketResponse();
        response.setType("document_progress");
        response.setMessage(status);
        response.setData(Map.of(
                "documentId", documentId,
                "status", status,
                "processed", processed,
                "total", total,
                "progress", String.format("%.1f%%", (double) processed / total * 100)
        ));
        sendMessage(objectMapper.writeValueAsString(response));
    }

    private void sendDocumentErrorMessage(Long documentId, String errorMessage) throws IOException {
        WebSocketResponse response = new WebSocketResponse();
        response.setType("document_error");
        response.setMessage(errorMessage);
        response.setData(Map.of("documentId", documentId, "error", errorMessage));
        sendMessage(objectMapper.writeValueAsString(response));
    }

    private void sendCompletionMessage(int totalDocuments) throws IOException {
        WebSocketResponse response = new WebSocketResponse();
        response.setType("completion");
        response.setMessage("所有文档处理完成");
        response.setData(Map.of("totalDocuments", totalDocuments, "timestamp", System.currentTimeMillis()));
        sendMessage(objectMapper.writeValueAsString(response));
    }

    private void sendErrorMessage(String errorMessage) throws IOException {
        WebSocketResponse response = new WebSocketResponse();
        response.setType("error");
        response.setMessage(errorMessage);
        sendMessage(objectMapper.writeValueAsString(response));
    }

    private void sendMessage(String message) throws IOException {
        Session session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            session.getBasicRemote().sendText(message);
            logger.debug("给用户 {} 发送消息：{}", userId, message);
        } else {
            logger.warn("用户 {} 的WebSocket连接已关闭或不存在，无法发送消息", userId);
        }
    }

    public static class FileParseRequestWrapper {
        private String type;
        private FileParseRequestData data;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public FileParseRequestData getData() {
            return data;
        }

        public void setData(FileParseRequestData data) {
            this.data = data;
        }
    }

    public static class FileParseRequestData {
        private Long kbId;
        private List<Long> documentIds;
        private String parseStrategy;
        private List<String> extractContent;
        private String segmentStrategy;

        public Long getKbId() {
            return kbId;
        }

        public void setKbId(Long kbId) {
            this.kbId = kbId;
        }

        public List<Long> getDocumentIds() {
            return documentIds;
        }

        public void setDocumentIds(List<Long> documentIds) {
            this.documentIds = documentIds;
        }

        public String getParseStrategy() {
            return parseStrategy;
        }

        public void setParseStrategy(String parseStrategy) {
            this.parseStrategy = parseStrategy;
        }

        public List<String> getExtractContent() {
            return extractContent;
        }

        public void setExtractContent(List<String> extractContent) {
            this.extractContent = extractContent;
        }

        public String getSegmentStrategy() {
            return segmentStrategy;
        }

        public void setSegmentStrategy(String segmentStrategy) {
            this.segmentStrategy = segmentStrategy;
        }
    }

    public static class WebSocketResponse {
        private String type;
        private String message;
        private Map<String, Object> data;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}