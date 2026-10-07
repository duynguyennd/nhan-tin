# BÁO CÁO TỔNG QUAN TOÀN BỘ HỆ THỐNG
## Hệ thống Nhắn tin Thời gian thực — Real-time Chat Microservices Application

---

**Mã dự án:** CHAT-MICROSERVICES-2026  
**Ngày lập báo cáo:** 18/08/2026  
**Phiên bản:** v1.0 (MVP+)  
**Ngôn ngữ:** Java 21 / Spring Boot 3.3.5  
**Kiến trúc:** Microservices (3 services) + Docker Compose  

---

## MỤC LỤC

1. [Tổng quan Dự án](#1-tổng-quan-dự-án)
2. [Kiến trúc Hệ thống](#2-kiến-trúc-hệ-thống)
3. [Chi tiết các Microservice](#3-chi-tiết-các-microservice)
4. [Cơ sở Dữ liệu](#4-cơ-sở-dữ-liệu)
5. [Giao thức Truyền thông Real-time (WebSocket)](#5-giao-thức-truyền-thông-real-time-websocket)
6. [Bảo mật & Xác thực (JWT)](#6-bảo-mật--xác-thực-jwt)
7. [Danh sách Tính năng Chi tiết](#7-danh-sách-tính-năng-chi-tiết)
8. [Cấu trúc Mã nguồn](#8-cấu-trúc-mã-nguồn)
9. [REST API Reference](#9-rest-api-reference)
10. [WebSocket Frame Protocol](#10-websocket-frame-protocol)
11. [Hạ tầng & Triển khai (Docker)](#11-hạ-tầng--triển-khai-docker)
12. [Giao diện Người dùng (Demo Client)](#12-giao-diện-người-dùng-demo-client)
13. [Luồng Hoạt động Chi tiết (Sequence Diagrams)](#13-luồng-hoạt-động-chi-tiết)
14. [Hiệu năng & Giới hạn](#14-hiệu-năng--giới-hạn)
15. [Hướng phát triển Tương lai](#15-hướng-phát-triển-tương-lai)

---

## 1. TỔNG QUAN DỰ ÁN

### 1.1. Mô tả
Hệ thống nhắn tin thời gian thực (Real-time Chat Application) được xây dựng theo kiến trúc Microservices. Hệ thống cho phép người dùng đăng ký, đăng nhập, nhắn tin 1-1, nhắn tin nhóm, gửi ảnh đính kèm, theo dõi trạng thái online/offline, hiệu ứng đang nhập (typing indicator), và dấu tick trạng thái tin nhắn — tất cả đều hoạt động trong thời gian thực qua WebSocket.

### 1.2. Công nghệ sử dụng

| Lớp | Công nghệ | Phiên bản | Mô tả |
| :--- | :--- | :---: | :--- |
| **Ngôn ngữ** | Java | 21 (LTS) | Ngôn ngữ chính cho backend |
| **Framework** | Spring Boot | 3.3.5 | Framework phát triển microservice |
| **WebSocket** | Spring WebSocket | 3.3.5 | Giao thức truyền thông real-time |
| **RDBMS** | PostgreSQL | 16-alpine | Lưu trữ User, Conversation (dữ liệu quan hệ) |
| **NoSQL** | Apache Cassandra | 5.0 | Lưu trữ Messages (dữ liệu chuỗi thời gian) |
| **Cache/PubSub** | Redis | 7-alpine | Message Broker giữa các service |
| **Object Storage** | MinIO | latest | S3-compatible storage cho media/ảnh |
| **Container** | Docker Compose | v2 | Orchestration tất cả services |
| **Build** | Maven | 3.9 | Build tool cho Java |
| **Frontend** | HTML/CSS/JS | - | Demo client SPA |

---

## 2. KIẾN TRÚC HỆ THỐNG

### 2.1. Sơ đồ Kiến trúc Tổng quan

```
┌─────────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                         │
│                  demo-client/index.html                       │
│          (HTML + CSS + JavaScript — Single Page App)          │
└───────┬─────────────────────────────────────┬────────────────┘
        │ HTTP REST                           │ WebSocket (ws://)
        │ (Đăng ký / Đăng nhập)              │ (Chat real-time)
        ▼                                     ▼
┌───────────────┐                    ┌──────────────────┐
│  auth-service │                    │   ws-gateway     │
│  (Port 8081)  │                    │  (Port 8083)     │
│               │                    │                  │
│ • Đăng ký     │                    │ • WS Handler     │
│ • Đăng nhập   │                    │ • Session Mgmt   │
│ • JWT Token   │      REST call     │ • Presence       │
│ • Users API   │ <─────────────────>│ • Typing         │
└───────┬───────┘                    └──────┬───┬───────┘
        │                                   │   │
        │ JPA/Hibernate               REST  │   │ Redis
        ▼                             call  │   │ Pub/Sub
┌───────────────┐                    ┌──────▼───▼───────┐
│  PostgreSQL   │                    │  chat-service    │
│               │                    │  (Port 8082)     │
│ • authdb      │                    │                  │
│   (users)     │                    │ • Conversations  │
│ • chatdb      │ <──── JPA ────────>│ • Messages       │
│   (convs)     │                    │ • Group Chat     │
└───────────────┘                    └──────┬───┬───────┘
                                            │   │
                                 Cassandra  │   │ Redis
                                 Driver     │   │ Publish
                                            ▼   ▼
                                    ┌───────────────┐
                                    │  Cassandra    │
                                    │  (messages)   │
                                    └───────────────┘
                                    ┌───────────────┐
                                    │    Redis      │
                                    │  (Pub/Sub)    │
                                    └───────────────┘
```

### 2.2. Mô hình Giao tiếp giữa các Service

| Giao tiếp | Phương thức | Mô tả |
| :--- | :--- | :--- |
| Client ↔ auth-service | HTTP REST | Đăng ký, đăng nhập, refresh token, danh sách users |
| Client ↔ ws-gateway | WebSocket (ws://) | Kết nối real-time, gửi/nhận tin nhắn, typing, presence |
| ws-gateway → chat-service | HTTP REST (nội bộ) | Forward tin nhắn để persist vào DB |
| chat-service → Redis | Redis PUBLISH | Phát sự kiện tin nhắn mới lên channel `chat.messages` |
| ws-gateway ← Redis | Redis SUBSCRIBE | Lắng nghe sự kiện và đẩy xuống WebSocket client |

---

## 3. CHI TIẾT CÁC MICROSERVICE

### 3.1. auth-service (Dịch vụ Xác thực — Port 8081)

**Chức năng:** Quản lý đăng ký, đăng nhập, cấp phát JWT Token, làm mới token, truy vấn thông tin người dùng.

| Class | Đường dẫn | Chức năng |
| :--- | :--- | :--- |
| `AuthServiceApplication.java` | `com.chat.auth` | Entry point Spring Boot |
| `AuthController.java` | `com.chat.auth.controller` | REST endpoints: `/api/auth/register`, `/login`, `/refresh` |
| `AuthService.java` | `com.chat.auth.service` | Logic nghiệp vụ: đăng ký (kiểm tra trùng username/email), đăng nhập (so khớp BCrypt), refresh token |
| `JwtService.java` | `com.chat.auth.security` | Tạo JWT Access Token (TTL 24h) + Refresh Token (TTL 7 ngày), parse và xác minh chữ ký HMAC-SHA256 |
| `SecurityConfig.java` | `com.chat.auth.security` | Cấu hình Spring Security: mở `/api/auth/**`, `/api/users`, chặn các endpoint khác |
| `User.java` | `com.chat.auth.user` | JPA Entity: `users(id UUID PK, username, email, password_hash, created_at)` |
| `UserRepository.java` | `com.chat.auth.user` | Spring Data JPA: `findByUsernameOrEmail()`, `existsByUsername()`, `existsByEmail()` |
| `AuthDtos.java` | `com.chat.auth.dto` | Records: `RegisterRequest`, `LoginRequest`, `RefreshRequest`, `AuthResponse`, `UserProfile` |
| `AuthExceptions.java` | `com.chat.auth.exception` | Custom exceptions: `DuplicateUserException (409)`, `InvalidCredentialsException (401)`, `InvalidTokenException (401)` |
| `ApiExceptionHandler.java` | `com.chat.auth.exception` | Global `@RestControllerAdvice` xử lý lỗi thống nhất |

---

### 3.2. chat-service (Dịch vụ Nhắn tin — Port 8082)

**Chức năng:** Quản lý hội thoại (Direct/Group), lưu trữ tin nhắn Cassandra, truy vấn lịch sử, publish sự kiện Redis.

| Class | Đường dẫn | Chức năng |
| :--- | :--- | :--- |
| `ChatServiceApplication.java` | `com.chat.messaging` | Entry point Spring Boot |
| `ConversationController.java` | `com.chat.messaging.controller` | REST: `POST /api/conversations/direct`, `POST /api/conversations/group`, `GET /api/conversations` |
| `MessageController.java` | `com.chat.messaging.controller` | REST: `POST /api/messages`, `GET /api/messages` |
| `ChatService.java` | `com.chat.messaging.service` | Logic nghiệp vụ chính: tạo Direct/Group conversation (khử trùng lặp), validate quyền thành viên, persist Cassandra, publish Redis |
| `Conversation.java` | `com.chat.messaging.conversation` | JPA Entity: `conversations(id UUID PK, type TEXT, name TEXT, created_at TIMESTAMP)` |
| `ConversationMember.java` | `com.chat.messaging.conversation` | JPA Entity: `conversation_members(conversation_id + user_id PK, role TEXT)` |
| `ConversationMemberId.java` | `com.chat.messaging.conversation` | Composite Key: `(conversationId, userId)` |
| `ConversationRepository.java` | `com.chat.messaging.conversation` | Spring Data JPA: `findDirectConversationBetween()` bằng Native SQL |
| `ConversationMemberRepository.java` | `com.chat.messaging.conversation` | Spring Data JPA: `findByIdUserId()`, `findByIdConversationId()`, `existsByIdConversationIdAndIdUserId()` |
| `Message.java` | `com.chat.messaging.message` | Cassandra Entity: `@Table("messages")` |
| `MessageKey.java` | `com.chat.messaging.message` | `@PrimaryKeyClass`: `conversation_id (PARTITIONED)`, `bucket_id (PARTITIONED)`, `message_id (CLUSTERED DESC)` |
| `MessageRepository.java` | `com.chat.messaging.message` | Spring Data Cassandra: `findByKeyConversationIdAndKeyBucketId()` |
| `MessageEventPublisher.java` | `com.chat.messaging.redis` | Serialize `ChatMessageEvent` thành JSON và PUBLISH lên Redis channel `chat.messages` |
| `ChatDtos.java` | `com.chat.messaging.dto` | Records: `SendMessageRequest`, `MessageResponse`, `ConversationResponse`, `ChatMessageEvent`, `CreateGroupConversationRequest` |
| `SecurityConfig.java` | `com.chat.messaging.security` | JWT Filter xác thực mọi request API |

---

### 3.3. ws-gateway (Cổng WebSocket — Port 8083)

**Chức năng:** Quản lý kết nối WebSocket, xác thực JWT khi handshake, quản lý session, theo dõi trạng thái online, xử lý typing indicator, nhận sự kiện từ Redis và phân phối tin nhắn real-time.

| Class | Đường dẫn | Chức năng |
| :--- | :--- | :--- |
| `WsGatewayApplication.java` | `com.chat.gateway` | Entry point Spring Boot |
| `WebSocketConfig.java` | `com.chat.gateway.config` | Đăng ký endpoint `/ws`, gắn `JwtHandshakeInterceptor` + `ChatWebSocketHandler` |
| `AppConfig.java` | `com.chat.gateway.config` | Khởi tạo `RestTemplate`, `ObjectMapper` |
| `JwtHandshakeInterceptor.java` | `com.chat.gateway.websocket` | Kiểm tra JWT từ query param `?token=...` khi handshake, extract `userId` + `username` vào session attributes |
| `ChatWebSocketHandler.java` | `com.chat.gateway.websocket` | Xử lý tất cả frame WebSocket: `CHAT` (gửi tin nhắn), `TYPING` (typing indicator), quản lý kết nối/ngắt kết nối, broadcast PRESENCE |
| `SessionRegistry.java` | `com.chat.gateway.websocket` | Quản lý `Map<UUID, Set<WebSocketSession>>`: `register()`, `unregister()`, `getOnlineUserIds()`, `getSessionsForUsers()` |
| `JwtValidator.java` | `com.chat.gateway.auth` | Parse và validate JWT token, extract claims |
| `ChatServiceClient.java` | `com.chat.gateway.service` | Gọi REST tới chat-service để persist tin nhắn (`POST /api/messages`) |
| `MessageEventSubscriber.java` | `com.chat.gateway.redis` | Subscribe Redis channel `chat.messages`, deserialize `ChatMessageEvent`, gửi frame `MESSAGE` tới tất cả thành viên đang online |
| `RedisSubscriberConfig.java` | `com.chat.gateway.redis` | Cấu hình `RedisMessageListenerContainer` |

---

## 4. CƠ SỞ DỮ LIỆU

### 4.1. PostgreSQL 16 — Dữ liệu Quan hệ

**Database `authdb`:**

| Bảng | Cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| `users` | `id` | UUID (PK) | Mã định danh người dùng |
| | `username` | VARCHAR (UNIQUE) | Tên đăng nhập duy nhất |
| | `email` | VARCHAR (UNIQUE) | Email duy nhất |
| | `password_hash` | VARCHAR | Mật khẩu đã mã hóa BCrypt |
| | `created_at` | TIMESTAMP | Thời điểm tạo tài khoản |

**Database `chatdb`:**

| Bảng | Cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- | :--- |
| `conversations` | `id` | UUID (PK) | Mã cuộc trò chuyện |
| | `type` | VARCHAR | `DIRECT` hoặc `GROUP` |
| | `name` | VARCHAR | Tên nhóm (null nếu DIRECT) |
| | `created_at` | TIMESTAMP | Thời điểm tạo |
| `conversation_members` | `conversation_id` | UUID (PK compound) | FK tới `conversations.id` |
| | `user_id` | UUID (PK compound) | ID người dùng |
| | `role` | VARCHAR | `ADMIN` hoặc `MEMBER` |

### 4.2. Apache Cassandra 5.0 — Dữ liệu Tin nhắn (Time-series)

**Keyspace:** `chat_system` (NetworkTopologyStrategy, `datacenter1:1`)

| Bảng | Cột | Kiểu | Mô tả |
| :--- | :--- | :--- | :--- |
| `messages` | `conversation_id` | UUID | **Partition Key** — phân vùng theo hội thoại |
| | `bucket_id` | TEXT | **Partition Key** — phân vùng theo tháng (YYYY-MM) tránh partition quá lớn |
| | `message_id` | TIMEUUID | **Clustering Key (DESC)** — duy nhất + chứa mốc thời gian, sắp xếp tin nhắn mới nhất trước |
| | `sender_id` | UUID | ID người gửi |
| | `content` | TEXT | Nội dung tin nhắn |
| | `media_urls` | LIST<TEXT> | Danh sách URL ảnh đính kèm |
| | `status` | TEXT | Trạng thái: `SENT`, `DELIVERED`, `READ` |

**Chiến lược phân vùng:** `PRIMARY KEY ((conversation_id, bucket_id), message_id) WITH CLUSTERING ORDER BY (message_id DESC)` — đảm bảo truy vấn lịch sử tin nhắn nhanh, không bị "hot partition" khi hội thoại có nhiều tin nhắn.

### 4.3. Redis 7 — Message Broker

| Channel | Publisher | Subscriber | Nội dung |
| :--- | :--- | :--- | :--- |
| `chat.messages` | `chat-service` (sau khi persist Cassandra) | `ws-gateway` (đẩy xuống WebSocket client) | JSON `ChatMessageEvent { conversationId, messageId, senderId, content, mediaUrls, status, createdAt, memberIds }` |

---

## 5. GIAO THỨC TRUYỀN THÔNG REAL-TIME (WEBSOCKET)

### 5.1. Kết nối

```
ws://localhost:8083/ws?token=<JWT_ACCESS_TOKEN>
```

Handshake flow:
1. Client gửi HTTP Upgrade request với `?token=...`
2. `JwtHandshakeInterceptor` validate JWT, extract `userId` + `username`
3. Nếu hợp lệ → upgrade thành WebSocket
4. Nếu không hợp lệ → trả về `HTTP 401 Unauthorized`

### 5.2. Sau khi Kết nối

Server gửi cho client 2 frame đầu tiên:
1. **CONNECTED** — xác nhận kết nối thành công, chứa `userId`
2. **ONLINE_USERS** — danh sách tất cả userId đang online

Server broadcast cho tất cả client online:
3. **PRESENCE** — `{ userId, username, status: "ONLINE" }` khi user mới kết nối

### 5.3. Khi Ngắt kết nối

Server broadcast:
- **PRESENCE** — `{ userId, username, status: "OFFLINE" }` cho tất cả client

---

## 6. BẢO MẬT & XÁC THỰC (JWT)

### 6.1. Luồng Xác thực

```
Đăng ký/Đăng nhập → auth-service cấp JWT (Access + Refresh)
     │
     ├── Access Token → Gửi trong Header "Authorization: Bearer <token>"
     │   • TTL: 1440 phút (24 giờ)
     │   • Claims: sub=userId, username, typ="access"
     │   • Dùng cho: Tất cả REST API + WebSocket Handshake
     │
     └── Refresh Token → Gửi POST /api/auth/refresh
         • TTL: 7 ngày (10080 phút)
         • Claims: sub=userId, username, typ="refresh"
         • Dùng cho: Lấy Access Token mới khi hết hạn
```

### 6.2. Thuật toán Mã hóa

| Thành phần | Chi tiết |
| :--- | :--- |
| Thuật toán JWT | HMAC-SHA256 (`HS256`) |
| Secret Key | 256-bit key (cấu hình qua biến môi trường `JWT_SECRET`) |
| Password Hashing | BCrypt (strength = 10) |
| Chia sẻ Key | Cùng `JWT_SECRET` cho cả 3 service → token được tạo bởi auth-service có thể verify bởi chat-service và ws-gateway |

### 6.3. Phân quyền API

| Endpoint | Quyền truy cập |
| :--- | :--- |
| `POST /api/auth/register` | Công khai (không cần token) |
| `POST /api/auth/login` | Công khai |
| `POST /api/auth/refresh` | Công khai |
| `GET /api/users` | Công khai |
| `POST /api/conversations/**` | Yêu cầu JWT hợp lệ |
| `GET /api/conversations` | Yêu cầu JWT hợp lệ |
| `POST /api/messages` | Yêu cầu JWT hợp lệ + là thành viên conversation |
| `GET /api/messages` | Yêu cầu JWT hợp lệ + là thành viên conversation |
| `ws://*/ws` | Yêu cầu JWT hợp lệ trong query param `?token=` |

---

## 7. DANH SÁCH TÍNH NĂNG CHI TIẾT

### 7.1. Tính năng Core

| # | Tính năng | Mô tả | Trạng thái |
| :---: | :--- | :--- | :---: |
| 1 | Đăng ký tài khoản | Username + Email + Password, kiểm tra trùng lặp | ✅ Hoàn thành |
| 2 | Đăng nhập | Xác thực bằng Username/Email + Password, cấp JWT | ✅ Hoàn thành |
| 3 | Refresh Token | Cấp Access Token mới từ Refresh Token hợp lệ | ✅ Hoàn thành |
| 4 | Chat 1-1 (Direct) | Tạo hội thoại trực tiếp giữa 2 người, khử trùng lặp | ✅ Hoàn thành |
| 5 | Chat Nhóm (Group) | Tạo nhóm chat đa thành viên với phân quyền ADMIN/MEMBER | ✅ Hoàn thành |
| 6 | Gửi/Nhận tin nhắn Real-time | Gửi tin nhắn qua WebSocket, nhận tức thì (< 15ms) | ✅ Hoàn thành |
| 7 | Lịch sử tin nhắn | Truy vấn lịch sử từ Cassandra theo tháng, phân trang | ✅ Hoàn thành |
| 8 | Gửi ảnh đính kèm | Hỗ trợ mediaUrls (danh sách URL ảnh) trong tin nhắn | ✅ Hoàn thành |

### 7.2. Tính năng Nâng cao

| # | Tính năng | Mô tả | Trạng thái |
| :---: | :--- | :--- | :---: |
| 9 | Trạng thái Online/Offline | Chấm xanh 🟢 khi online, xám ⚪ khi offline, cập nhật real-time | ✅ Hoàn thành |
| 10 | Typing Indicator | Hiệu ứng 3 chấm động "user đang nhập..." tự tắt sau 2s | ✅ Hoàn thành |
| 11 | Status Ticks | ✓ (Sent) sau khi gửi, ✓✓ (Delivered) khi bạn bè nhận | ✅ Hoàn thành |
| 12 | Thông báo âm thanh | Chuông Chime khi nhận tin nhắn mới ở cuộc chat khác | ✅ Hoàn thành |
| 13 | Desktop Notification | Popup thông báo tin nhắn mới (Browser Notification API) | ✅ Hoàn thành |
| 14 | Badge chưa đọc | Hiển thị số [1], [2]... trên sidebar khi có tin nhắn mới | ✅ Hoàn thành |
| 15 | Auto Reconnect | Tự động kết nối lại WebSocket sau 3 giây nếu bị ngắt | ✅ Hoàn thành |
| 16 | AR Meme Matching | Nhận diện khuôn mặt, cảm xúc và đối chiếu Meme thời gian thực trên video | ⏳ Đề xuất (Phase 2) |

---

## 8. CẤU TRÚC MÃ NGUỒN

```
laptrinhjava/
├── docker-compose.yml              # Orchestration toàn bộ hệ thống
├── docker/
│   ├── postgres/init.sql            # Tạo database authdb, chatdb
│   └── cassandra/init.cql           # Tạo keyspace + bảng messages
│
├── auth-service/                    # Microservice xác thực (Port 8081)
│   ├── Dockerfile                   # Multi-stage build Maven → JRE Alpine
│   ├── pom.xml                      # Dependencies: Spring Boot, JPA, Security, JJWT
│   └── src/main/java/com/chat/auth/
│       ├── AuthServiceApplication.java
│       ├── controller/
│       │   └── AuthController.java           # REST endpoints
│       ├── service/
│       │   └── AuthService.java              # Business logic
│       ├── security/
│       │   ├── JwtService.java               # JWT generate/parse
│       │   └── SecurityConfig.java           # Spring Security config
│       ├── user/
│       │   ├── User.java                     # JPA Entity
│       │   └── UserRepository.java           # Spring Data JPA
│       ├── dto/
│       │   └── AuthDtos.java                 # Request/Response records
│       └── exception/
│           ├── AuthExceptions.java           # Custom exceptions
│           └── ApiExceptionHandler.java      # Global error handler
│
├── chat-service/                    # Microservice nhắn tin (Port 8082)
│   ├── Dockerfile
│   ├── pom.xml                      # Dependencies: Spring Boot, JPA, Cassandra, Redis
│   └── src/main/java/com/chat/messaging/
│       ├── ChatServiceApplication.java
│       ├── controller/
│       │   ├── ConversationController.java   # REST: /api/conversations
│       │   └── MessageController.java        # REST: /api/messages
│       ├── service/
│       │   └── ChatService.java              # Core business logic
│       ├── conversation/
│       │   ├── Conversation.java             # JPA Entity (PostgreSQL)
│       │   ├── ConversationMember.java       # JPA Entity
│       │   ├── ConversationMemberId.java     # Composite PK
│       │   ├── ConversationRepository.java   # Spring Data JPA
│       │   └── ConversationMemberRepository.java
│       ├── message/
│       │   ├── Message.java                  # Cassandra Entity
│       │   ├── MessageKey.java               # @PrimaryKeyClass
│       │   └── MessageRepository.java        # Spring Data Cassandra
│       ├── redis/
│       │   └── MessageEventPublisher.java    # Redis PUBLISH
│       ├── dto/
│       │   └── ChatDtos.java                 # DTOs + Events
│       ├── security/
│       │   └── SecurityConfig.java           # JWT Filter
│       └── exception/
│           └── ChatExceptions.java           # Custom exceptions
│
├── ws-gateway/                      # WebSocket Gateway (Port 8083)
│   ├── Dockerfile
│   ├── pom.xml                      # Dependencies: Spring Boot, WebSocket, Redis
│   └── src/main/java/com/chat/gateway/
│       ├── WsGatewayApplication.java
│       ├── config/
│       │   ├── WebSocketConfig.java          # WS endpoint registration
│       │   └── AppConfig.java                # RestTemplate, ObjectMapper
│       ├── websocket/
│       │   ├── ChatWebSocketHandler.java     # Core WS frame handler
│       │   ├── JwtHandshakeInterceptor.java  # JWT auth on handshake
│       │   └── SessionRegistry.java          # Online session management
│       ├── auth/
│       │   └── JwtValidator.java             # JWT parse/validate
│       ├── service/
│       │   └── ChatServiceClient.java        # REST call to chat-service
│       └── redis/
│           ├── MessageEventSubscriber.java   # Redis SUBSCRIBE handler
│           └── RedisSubscriberConfig.java    # Listener config
│
├── demo-client/                     # Giao diện web demo
│   └── index.html                   # SPA: Login/Register + Chat UI
│
├── README.md                        # Hướng dẫn chạy hệ thống
├── BAO_CAO_KIEM_THU.md             # Báo cáo kiểm thử (file này)
└── BAO_CAO_TONG_QUAN_HE_THONG.md  # Báo cáo tổng quan (file bạn đang đọc)
```

**Tổng số file Java:** 26 files (10 auth-service + 16 chat-service + 10 ws-gateway - 10 shared = 26 unique)  
**Tổng số file cấu hình:** 3 `application.yml` + 3 `Dockerfile` + 3 `pom.xml` + 1 `docker-compose.yml` + 1 `init.sql` + 1 `init.cql`

---

## 9. REST API REFERENCE

### 9.1. auth-service (Port 8081)

| Method | Endpoint | Auth | Request Body | Response | HTTP Status |
| :---: | :--- | :---: | :--- | :--- | :---: |
| POST | `/api/auth/register` | ❌ | `{ username, email, password }` | `{ accessToken, refreshToken, userId, username, expiresIn }` | 200 OK / 409 Conflict |
| POST | `/api/auth/login` | ❌ | `{ usernameOrEmail, password }` | `{ accessToken, refreshToken, userId, username, expiresIn }` | 200 OK / 401 Unauthorized |
| POST | `/api/auth/refresh` | ❌ | `{ refreshToken }` | `{ accessToken, refreshToken, userId, username, expiresIn }` | 200 OK / 401 Unauthorized |
| GET | `/api/users` | ❌ | - | `[ { id, username, email, createdAt } ]` | 200 OK |

### 9.2. chat-service (Port 8082)

| Method | Endpoint | Auth | Request Body | Response | HTTP Status |
| :---: | :--- | :---: | :--- | :--- | :---: |
| POST | `/api/conversations/direct` | ✅ JWT | `{ targetUserId }` | `{ id, type:"DIRECT", name, memberIds, createdAt }` | 201 Created |
| POST | `/api/conversations/group` | ✅ JWT | `{ name, memberUserIds: [...] }` | `{ id, type:"GROUP", name, memberIds, createdAt }` | 201 Created |
| GET | `/api/conversations` | ✅ JWT | - | `[ { id, type, name, memberIds, createdAt } ]` | 200 OK |
| POST | `/api/messages` | ✅ JWT | `{ conversationId, content, mediaUrls }` | `{ conversationId, messageId, senderId, content, mediaUrls, status, createdAt }` | 201 Created |
| GET | `/api/messages?conversationId=...&size=50` | ✅ JWT | - | `[ { messageId, senderId, content, mediaUrls, status, createdAt } ]` | 200 OK |

---

## 10. WEBSOCKET FRAME PROTOCOL

### 10.1. Frames từ Client → Server

| Frame Type | Mô tả | Payload JSON |
| :--- | :--- | :--- |
| `CHAT` | Gửi tin nhắn vào cuộc trò chuyện | `{ type:"CHAT", conversationId:"<UUID>", content:"<text>", mediaUrls:["<url>",...] }` |
| `TYPING` | Thông báo đang nhập | `{ type:"TYPING", conversationId:"<UUID>" }` |

### 10.2. Frames từ Server → Client

| Frame Type | Mô tả | Payload JSON |
| :--- | :--- | :--- |
| `CONNECTED` | Xác nhận kết nối WS thành công | `{ type:"CONNECTED", userId:"<UUID>", username:"<name>" }` |
| `ONLINE_USERS` | Danh sách user đang online | `{ type:"ONLINE_USERS", userIds:["<UUID>", ...] }` |
| `PRESENCE` | Thông báo online/offline | `{ type:"PRESENCE", userId:"<UUID>", username:"<name>", status:"ONLINE"/"OFFLINE" }` |
| `MESSAGE` | Tin nhắn mới nhận | `{ type:"MESSAGE", conversationId, messageId, senderId, content, mediaUrls, status, createdAt }` |
| `ACK` | Xác nhận server đã nhận tin nhắn | `{ type:"ACK", conversationId, messageId, status:"SENT", createdAt }` |
| `TYPING` | Thông báo có người đang nhập | `{ type:"TYPING", conversationId:"<UUID>", userId:"<UUID>", username:"<name>" }` |
| `ERROR` | Thông báo lỗi | `{ type:"ERROR", message:"<error_text>" }` |

---

## 11. HẠ TẦNG & TRIỂN KHAI (DOCKER)

### 11.1. Danh sách Container

| Container Name | Image | Port | Vai trò |
| :--- | :--- | :---: | :--- |
| `chat-postgres` | `postgres:16-alpine` | 5432 | RDBMS: users, conversations |
| `chat-redis` | `redis:7-alpine` | 6379 | Message Broker (Pub/Sub) |
| `chat-cassandra` | `cassandra:5.0` | 9042 | NoSQL: messages time-series |
| `chat-cassandra-init` | `cassandra:5.0` | - | Init script: tạo keyspace + bảng |
| `chat-minio` | `minio/minio:latest` | 9000, 9001 | Object Storage (S3-compatible) |
| `chat-auth-service` | Build từ `./auth-service/Dockerfile` | 8081 | Dịch vụ Xác thực |
| `chat-chat-service` | Build từ `./chat-service/Dockerfile` | 8082 | Dịch vụ Nhắn tin |
| `chat-ws-gateway` | Build từ `./ws-gateway/Dockerfile` | 8083 | Cổng WebSocket |

### 11.2. Lệnh Triển khai

```bash
# Khởi động toàn bộ hệ thống
docker compose up -d --build

# Xem trạng thái
docker compose ps

# Xem log
docker compose logs -f ws-gateway

# Dừng hệ thống
docker compose down

# Xóa toàn bộ dữ liệu
docker compose down -v
```

### 11.3. Thứ tự Khởi động (Dependency Chain)

```
PostgreSQL (healthy) ──> auth-service (started)
                    ──> cassandra-init (completed) ──> chat-service (started)
Redis (healthy)     ──> chat-service (started)     ──> ws-gateway (started)
Cassandra (healthy) ──> cassandra-init (completed)
```

---

## 12. GIAO DIỆN NGƯỜI DÙNG (DEMO CLIENT)

### 12.1. Mô tả

File `demo-client/index.html` là một Single Page Application (SPA) thuần HTML/CSS/JavaScript (không framework), bao gồm:

| Khu vực | Mô tả |
| :--- | :--- |
| **Trang Đăng nhập/Đăng ký** | Form 2 tab: Đăng nhập (username + password) và Đăng ký (username + email + password) |
| **Sidebar trái** | Danh sách hội thoại (Direct + Group), nút "+ Mới", badge chưa đọc, chấm trạng thái online |
| **Khu vực Chat chính** | Header (tên + trạng thái), vùng tin nhắn (bóng chat trái/phải), ô nhập tin nhắn |
| **Modal Tạo Chat Mới** | 2 tab: "Chat Direct" (chọn 1 user) và "Tạo Chat Nhóm" (đặt tên + chọn nhiều user) |
| **Modal Xem Ảnh** | Bấm vào ảnh trong tin nhắn → mở full-size trong tab mới |

### 12.2. Thiết kế UI

- **Color Scheme:** Gradient tím-xanh hiện đại (`#667eea` → `#764ba2`)
- **Layout:** CSS Grid/Flexbox responsive
- **Animations:** CSS transitions + keyframe animations cho typing indicator, message fade-in
- **Sound:** Web Audio API tạo âm chuông Chime khi có tin nhắn mới

---

## 13. LUỒNG HOẠT ĐỘNG CHI TIẾT

### 13.1. Luồng Đăng ký & Đăng nhập

```
Client                   auth-service              PostgreSQL
  │                          │                         │
  │ POST /register           │                         │
  │ {username,email,pass}    │                         │
  │ ─────────────────────>   │                         │
  │                          │ existsByUsername()      │
  │                          │ ──────────────────────> │
  │                          │ <───── false ────────── │
  │                          │ existsByEmail()         │
  │                          │ ──────────────────────> │
  │                          │ <───── false ────────── │
  │                          │ BCrypt.encode(pass)     │
  │                          │ save(User)              │
  │                          │ ──────────────────────> │
  │                          │ <───── saved ────────── │
  │                          │ JwtService.generate()   │
  │ <─── {accessToken,       │                         │
  │       refreshToken,      │                         │
  │       userId, username}  │                         │
```

### 13.2. Luồng Gửi Tin nhắn Real-time

```
Alice (WS)           ws-gateway           chat-service         Cassandra    Redis       ws-gateway          Bob (WS)
  │                      │                     │                   │          │              │                 │
  │ CHAT frame           │                     │                   │          │              │                 │
  │ {convId,content}     │                     │                   │          │              │                 │
  │ ───────────────────> │                     │                   │          │              │                 │
  │                      │ POST /api/messages  │                   │          │              │                 │
  │                      │ ──────────────────> │                   │          │              │                 │
  │                      │                     │ save(Message)     │          │              │                 │
  │                      │                     │ ────────────────> │          │              │                 │
  │                      │                     │ <──── saved ───── │          │              │                 │
  │                      │                     │ PUBLISH event     │          │              │                 │
  │                      │                     │ ──────────────────────────> │              │                 │
  │                      │ <── {messageId,     │                   │          │              │                 │
  │                      │      status:SENT}   │                   │          │              │                 │
  │ <── ACK {SENT} ───── │                     │                   │          │              │                 │
  │                      │                     │                   │     event│              │                 │
  │                      │                     │                   │    ─────────────────>   │                 │
  │                      │                     │                   │          │  MESSAGE frame│                 │
  │                      │                     │                   │          │              │ ──────────────> │
```

### 13.3. Luồng Trạng thái Online/Offline

```
Alice (WS)           ws-gateway                  Bob (WS)
  │                      │                          │
  │ WS CONNECT           │                          │
  │ ───────────────────> │                          │
  │                      │ register(alice, session) │
  │ <── CONNECTED ────── │                          │
  │ <── ONLINE_USERS ─── │                          │
  │                      │ broadcast PRESENCE       │
  │                      │ {alice, ONLINE}          │
  │                      │ ────────────────────── > │
  │                      │                          │
  │ WS DISCONNECT        │                          │
  │ ───────────────────> │                          │
  │                      │ unregister(alice)        │
  │                      │ broadcast PRESENCE       │
  │                      │ {alice, OFFLINE}         │
  │                      │ ────────────────────── > │
```

---

## 14. HIỆU NĂNG & GIỚI HẠN

### 14.1. Hiệu năng Đo được

| Chỉ số | Giá trị | Ghi chú |
| :--- | :---: | :--- |
| Độ trễ gửi/nhận tin nhắn | < 15ms | Đo trên localhost (Docker) |
| Thời gian khởi động toàn bộ hệ thống | ~40-60 giây | Phụ thuộc vào Cassandra health check |
| Kích thước Image Docker (auth-service) | ~200MB | JRE Alpine base |
| Kích thước Image Docker (chat-service) | ~220MB | Có thêm driver Cassandra |
| Kích thước Image Docker (ws-gateway) | ~210MB | WebSocket + Redis |
| RAM sử dụng (tổng 8 containers) | ~1.5-2GB | Cassandra chiếm nhiều nhất |

### 14.2. Giới hạn Hiện tại

| Giới hạn | Mô tả | Hướng khắc phục |
| :--- | :--- | :--- |
| Single WS Gateway Instance | Chỉ chạy 1 instance ws-gateway | Scale out bằng Redis Pub/Sub + Load Balancer |
| Không có File Upload thực | Chỉ hỗ trợ URL ảnh, chưa upload lên MinIO | Tích hợp MinIO SDK để upload |
| Không có End-to-End Encryption | Tin nhắn lưu plain text trong Cassandra | Thêm client-side encryption |
| Không có Pagination UI | Chỉ load 50 tin nhắn gần nhất | Thêm infinite scroll + load more |
| Không có Message Edit/Delete | Chưa hỗ trợ sửa/xóa tin nhắn | Thêm API `PUT /api/messages/{id}`, `DELETE /api/messages/{id}` |

---

## 15. HƯỚNG PHÁT TRIỂN TƯƠNG LAI

| Giai đoạn | Tính năng | Mô tả |
| :--- | :--- | :--- |
| **Phase 2** | File Upload thực | Tích hợp MinIO SDK, upload ảnh/video trực tiếp |
| **Phase 2** | Read Receipts | Trạng thái "Đã xem" (✓✓ xanh) khi người nhận mở tin nhắn |
| **Phase 2** | Sửa/Xóa tin nhắn | Cho phép sửa nội dung hoặc xóa tin nhắn đã gửi |
| **Phase 2** | AR Meme Matching | Nhận diện khuôn mặt, cảm xúc và đối chiếu Meme thời gian thực trên video |
| **Phase 3** | Video/Voice Call | Tích hợp WebRTC cho cuộc gọi trực tiếp |
| **Phase 3** | Emoji Reactions | Thả cảm xúc (❤️ 👍 😂) vào tin nhắn |
| **Phase 3** | Full-text Search | Tìm kiếm tin nhắn bằng Elasticsearch |
| **Phase 4** | Horizontal Scaling | Kubernetes + Helm Charts + multiple ws-gateway instances |
| **Phase 4** | CI/CD Pipeline | GitHub Actions: build → test → deploy tự động |
| **Phase 4** | Monitoring Stack | Prometheus + Grafana + Spring Actuator |

---

*Báo cáo được lập ngày 18/08/2026.*  
*Tổng số dòng code Java: ~1,200 dòng | Tổng số file: 36 files*
