# Flink 话单聚合系统

## 项目概述

这是一个基于 Apache Flink 的实时话单数据处理系统，从 Kafka 消费电信话单数据，进行解析、验证、聚合，并将结果输出到 Kafka。

## 功能特性

1. **Kafka 数据接入**
   - 从指定 Kafka topic 消费 JSON 格式话单数据
   - 支持命令行参数动态配置 Kafka 集群地址
   - 支持自定义消费者组 ID 和并行度

2. **数据解析与验证**
   - 高性能 JSON 解析器
   - 严格的字段验证：
     - `bill_id`: 字符串类型，非空且唯一
     - `timestamp`: 时间戳类型，支持多种日期格式
     - `caller_number`: 字符串类型，符合 E.164 电话号码格式标准
     - `usage_amount`: 数值类型，非负且精度符合业务要求
     - `service_type`: 字符串类型，必须为预定义枚举值之一（VOICE, SMS, DATA, MMS, ROAMING, VALUE_ADDED）

3. **异常处理机制**
   - 使用 Flink 侧输出流（Side Output）机制
   - 分别路由 JSON 解析失败记录和字段验证失败记录
   - 异常记录包含原始数据、错误类型及详细错误原因
   - 异常数据输出到 `billing-bad-records` topic

4. **数据聚合逻辑**
   - 基于 `caller_number` 字段和 `timestamp` 字段（按 "yyyy-MM" 格式转换）的双重分组
   - 基于事件时间（Event Time）的窗口聚合策略
   - 累计计算每个分组的 `usage_amount` 总和
   - 水印（Watermark）生成策略处理数据延迟

5. **结果输出**
   - 聚合结果转换为 JSON 格式，包含以下字段：
     - `caller_number`: 用户电话号码
     - `month`: yyyy-MM 格式的统计月份
     - `total_usage`: 该用户当月总使用量
     - `record_count`: 该用户当月记录总数
     - `calculated_at`: 计算结果生成的时间戳
   - 输出到 `billing-accumulate` topic

## 项目结构

```
src/main/java/com/example/websocketchatbacked/
├── flink/
│   ├── job/
│   │   └── BillingAggregationJob.java    # Flink 作业主类
│   ├── model/
│   │   ├── BillRecord.java                 # 话单数据实体
│   │   ├── ServiceType.java                # 服务类型枚举
│   │   ├── ValidationError.java             # 验证错误实体
│   │   └── AggregateResult.java           # 聚合结果实体
│   ├── parser/
│   │   └── BillRecordParser.java           # JSON 解析和验证器
│   └── util/
│       └── TestDataGenerator.java           # 测试数据生成器
└── config/
    └── WebSocketConfig.java
```

## 配置说明

### application.yml 配置

```yaml
flink:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: flink-billing-consumer-group
      auto-offset-reset: earliest
      parallelism: 1
    producer:
      parallelism: 1
    topics:
      input: telecom-bill
      output: billing-accumulate
      error: billing-bad-records
    window:
      size: 1
      unit: days
      slide: 1
      slide-unit: days
    watermark:
      max-out-of-orderness: 5
      unit: minutes
```

### 命令行参数

运行 Flink 作业时支持以下命令行参数：

```
java -jar your-app.jar <bootstrap-servers> <input-topic> <output-topic> <error-topic> <parallelism>
```

参数说明：
- `bootstrap-servers`: Kafka 集群地址，格式为 "host:port,host:port"（默认: localhost:9092）
- `input-topic`: 输入 topic 名称（默认: telecom-bill）
- `output-topic`: 输出 topic 名称（默认: billing-accumulate）
- `error-topic`: 错误 topic 名称（默认: billing-bad-records）
- `parallelism`: 并行度（默认: 1）

## 使用方法

### 1. 启动 Kafka 集群

确保 Kafka 集群已启动并创建必要的 topic：

```bash
# 创建 topic
kafka-topics.sh --create --topic telecom-bill --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic billing-accumulate --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic billing-bad-records --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### 2. 生成测试数据

使用测试数据生成器生成测试数据：

```bash
# 生成 100 条测试数据
java -cp your-app.jar com.example.websocketchatbacked.flink.util.TestDataGenerator localhost:9092 telecom-bill 100
```

### 3. 运行 Flink 作业

```bash
# 使用默认配置运行
java -cp your-app.jar com.example.websocketchatbacked.flink.job.BillingAggregationJob

# 使用自定义配置运行
java -cp your-app.jar com.example.websocketchatbacked.flink.job.BillingAggregationJob \
  kafka1:9092,kafka2:9092 \
  telecom-bill \
  billing-accumulate \
  billing-bad-records \
  2
```

### 4. 查看结果

```bash
# 查看聚合结果
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic billing-accumulate --from-beginning

# 查看错误记录
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic billing-bad-records --from-beginning
```

## 测试

### 单元测试

运行单元测试验证各个组件：

```bash
mvn test -Dtest=RealKafkaSourceTest
```

### 集成测试

运行集成测试验证端到端流程：

```bash
# 1. 启动 Kafka
# 2. 生成测试数据
java -cp your-app.jar com.example.websocketchatbacked.flink.util.TestDataGenerator localhost:9092 telecom-bill 50

# 3. 运行 Flink 作业
java -cp your-app.jar com.example.websocketchatbacked.flink.job.BillingAggregationJob localhost:9092

# 4. 验证结果
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic billing-accumulate --from-beginning
```

## 数据格式

### 输入数据格式（telecom-bill topic）

```json
{
  "bill_id": "B001",
  "user_id": "U001",
  "caller_number": "+8613800138000",
  "timestamp": "2025-02-09T10:00:00",
  "usage_amount": 150.50,
  "service_type": "VOICE"
}
```

### 输出数据格式（billing-accumulate topic）

```json
{
  "caller_number": "+8613800138000",
  "month": "2025-02",
  "total_usage": 150.50,
  "record_count": 1,
  "calculated_at": "2025-02-09T10:05:00"
}
```

### 错误数据格式（billing-bad-records topic）

```json
{
  "errorType": "INVALID_FORMAT",
  "errorMessage": "caller_number不符合E.164格式: invalid",
  "fieldName": "caller_number",
  "originalData": "{\"bill_id\":\"B001\",\"caller_number\":\"invalid\"}",
  "timestamp": 1739085900000
}
```

## 性能优化

1. **并行度配置**: 根据数据量和 Kafka 分区数调整并行度
2. **窗口大小**: 根据业务需求调整窗口大小和滑动间隔
3. **水印策略**: 根据数据延迟情况调整水印的最大乱序时间
4. **Kafka 分区**: 确保分区数与 Flink 并行度匹配

## 故障排查

### 常见问题

1. **连接 Kafka 失败**
   - 检查 Kafka 集群是否启动
   - 验证 bootstrap-servers 配置是否正确
   - 检查网络连通性

2. **数据解析失败**
   - 检查输入数据格式是否符合要求
   - 查看错误 topic 中的详细错误信息

3. **聚合结果不准确**
   - 检查水印策略配置
   - 验证时间戳提取逻辑
   - 查看窗口配置是否合理

## 依赖项

- Apache Flink 1.16.3
- Apache Kafka 3.6.0
- Jackson 2.x（JSON 处理）
- Spring Boot 3.5.0

## 许可证

本项目仅供学习和参考使用。
