# SA-Token 登录权限控制系统实现文档

## 项目概述

本项目基于 Spring Boot 3.5.0 + SA-Token 1.39.0 + Redis + MySQL 实现了一套完整的登录权限控制系统，确保与现有 WebSocket 聊天功能完全解耦。

## 技术栈

- Spring Boot 3.5.0
- SA-Token 1.39.0
- Redis (用于Token存储)
- MySQL 9.1.0
- JPA/Hibernate
- Java 17

## 项目结构

```
src/main/java/com/example/websocketchatbacked/
├── config/
│   ├── MySaTokenConfig.java      # SA-Token权限配置类
│   └── WebSocketConfig.java      # WebSocket配置（原有）
├── controller/
│   ├── AuthController.java       # 登录认证接口
│   ├── DemoController.java        # 权限控制演示接口
│   └── WebSocketChatEndpoint.java # WebSocket端点（原有）
├── dto/
│   ├── ApiResponse.java           # 统一响应格式
│   └── LoginRequest.java          # 登录请求DTO
├── entity/
│   └── User.java                 # 用户实体类
├── exception/
│   └── GlobalExceptionHandler.java # 全局异常处理
└── repository/
    └── UserRepository.java        # 用户数据访问层
```

## 核心功能实现

### 1. 依赖配置 (pom.xml)

已添加以下依赖：
- `sa-token-spring-boot3-starter` 1.39.0
- `sa-token-redis-jackson` 1.39.0
- `spring-boot-starter-data-redis`
- `spring-boot-starter-data-jpa`
- `mysql-connector-j` 9.1.0

### 2. 配置文件 (application.yml)

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chat_system
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
  data:
    redis:
      host: localhost
      port: 6379

# SA-Token配置
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  store-type: redis
  is-concurrent: false
  timeout: 86400  # 24小时
```

### 3. 权限接口实现 (MySaTokenConfig.java)

实现 `StpInterface` 接口，根据用户ID查询权限：
- 权限值：1=只写，2=只读，3=读写

### 4. 登录接口 (AuthController.java)

提供以下接口：
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `GET /api/auth/info` - 获取用户信息

### 5. 权限控制注解

- `@SaCheckLogin` - 需要登录
- `@SaCheckPermission("3")` - 需要特定权限

### 6. 全局异常处理 (GlobalExceptionHandler.java)

统一处理认证异常：
- `NotLoginException` - 返回 401 "请先登录"
- `NotPermissionException` - 返回 403 "权限不足，无法访问"

## 数据库初始化

执行 `src/main/resources/sql/init.sql` 脚本：

```sql
-- 创建数据库和表
-- 插入测试用户：
-- admin/admin123 (权限3-读写)
-- writer/writer123 (权限1-只写)
-- reader/reader123 (权限2-只读)
```

## API 接口说明

### 登录接口

**请求：**
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**响应：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "Bearer xxxx-xxxx-xxxx-xxxx"
  }
}
```

### 访问受保护接口

**请求头：**
```
Authorization: Bearer {token}
```

### 权限控制演示接口

- `GET /api/demo/public` - 公开接口，无需登录
- `GET /api/demo/protected` - 需要登录
- `GET /api/demo/write` - 需要写权限
- `GET /api/demo/read` - 需要读权限
- `GET /api/demo/full` - 需要读写权限

## 使用示例

### 1. 登录获取Token

```bash
curl -X POST http://localhost:7676/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 2. 使用Token访问受保护接口

```bash
curl http://localhost:7676/api/demo/protected \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 3. 测试权限控制

```bash
# 测试读写权限接口
curl http://localhost:7676/api/demo/full \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

## 安全特性

1. **单点登录**：`is-concurrent: false`，新登录会使旧token失效
2. **Token过期**：24小时自动过期
3. **Redis存储**：Token存储在Redis中，支持分布式部署
4. **权限细粒度控制**：支持不同权限级别的接口访问控制
5. **统一异常处理**：友好的错误提示信息

## 与现有代码的解耦

- 认证系统完全独立，不影响原有WebSocket聊天功能
- WebSocket端点 (`/ws/chat`) 仍然可以正常访问
- 新增的RESTful API路径使用 `/api` 前缀，避免冲突

## 注意事项

1. 确保Redis服务已启动并运行在 localhost:6379
2. 确保MySQL服务已启动并创建了 chat_system 数据库
3. 首次运行需要执行数据库初始化脚本
4. 生产环境建议修改以下配置：
   - Redis密码
   - 数据库密码
   - SA-Token的jwt-secret-key

## 扩展建议

1. 添加密码加密（如BCrypt）
2. 实现用户注册接口
3. 添加Token刷新机制
4. 实现更复杂的权限模型（如角色+权限）
5. 添加操作日志记录
6. 实现WebSocket连接的认证集成

## 编译和运行

```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run
```

项目启动后，访问 http://localhost:7676 即可使用相关接口。
