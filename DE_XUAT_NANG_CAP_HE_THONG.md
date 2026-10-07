# ĐỀ XUẤT NÂNG CẤP HỆ THỐNG
## Hệ thống Nhắn tin Thời gian thực — Real-time Chat Microservices

---

**Mã dự án:** CHAT-MICROSERVICES-2026  
**Ngày lập:** 18/08/2026  
**Phiên bản hiện tại:** v1.0 (MVP+)  
**Phiên bản đề xuất:** v2.0 → v3.0 → v4.0  

---

## MỤC LỤC

1. [Đánh giá Hệ thống Hiện tại](#1-đánh-giá-hệ-thống-hiện-tại)
2. [Lộ trình Nâng cấp Tổng quan](#2-lộ-trình-nâng-cấp-tổng-quan)
3. [PHASE 1: Hoàn thiện Core (v1.5)](#3-phase-1-hoàn-thiện-core-v15)
4. [PHASE 2: Tính năng Nâng cao (v2.0)](#4-phase-2-tính-năng-nâng-cao-v20)
5. [PHASE 3: Enterprise & Scale (v3.0)](#5-phase-3-enterprise--scale-v30)
6. [PHASE 4: Production-Ready (v4.0)](#6-phase-4-production-ready-v40)
7. [Nâng cấp Kiến trúc](#7-nâng-cấp-kiến-trúc)
8. [Nâng cấp Bảo mật](#8-nâng-cấp-bảo-mật)
9. [Nâng cấp Hiệu năng](#9-nâng-cấp-hiệu-năng)
10. [Nâng cấp DevOps & Vận hành](#10-nâng-cấp-devops--vận-hành)
11. [Ước tính Nguồn lực](#11-ước-tính-nguồn-lực)
12. [Bảng So sánh Trước & Sau](#12-bảng-so-sánh-trước--sau)

---

## 1. ĐÁNH GIÁ HỆ THỐNG HIỆN TẠI

### 1.1. Điểm mạnh (Giữ nguyên)

| # | Điểm mạnh | Chi tiết |
| :---: | :--- | :--- |
| 1 | Kiến trúc Microservices rõ ràng | 3 service tách biệt (auth, chat, ws-gateway) theo nguyên tắc Single Responsibility |
| 2 | Lựa chọn DB phù hợp | PostgreSQL cho dữ liệu quan hệ, Cassandra cho dữ liệu time-series |
| 3 | Real-time qua WebSocket | Độ trễ < 15ms, hỗ trợ presence, typing indicator |
| 4 | JWT Authentication | Stateless, scalable, Access + Refresh Token |
| 5 | Docker Compose | Triển khai 1 lệnh, dependency chain rõ ràng |
| 6 | Cassandra Bucketing | Chiến lược `bucket_id = YYYY-MM` tránh hot partition |

### 1.2. Điểm yếu cần Nâng cấp

| # | Điểm yếu | Mức độ | Ảnh hưởng |
| :---: | :--- | :---: | :--- |
| 1 | Single instance ws-gateway | Nghiêm trọng | Không thể scale horizontal, single point of failure |
| 2 | Không có Health Check endpoint | Trung bình | Không giám sát được trạng thái service |
| 3 | Không có Rate Limiting | Nghiêm trọng | Dễ bị DDoS, spam tin nhắn |
| 4 | Không có API Gateway | Trung bình | Client phải biết nhiều port (8081, 8082, 8083) |
| 5 | Không có File Upload thực | Trung bình | Chỉ hỗ trợ URL ảnh, chưa upload binary |
| 6 | Không có Unit Test tự động | Nghiêm trọng | Không có regression test khi thay đổi code |
| 7 | Không có Logging tập trung | Trung bình | Khó trace lỗi giữa nhiều service |
| 8 | Không có E2E Encryption | Thấp | Tin nhắn lưu plain text |
| 9 | Không có Message Edit/Delete | Thấp | Thiếu tính năng cơ bản |
| 10 | Không có CI/CD Pipeline | Trung bình | Deploy thủ công |

### 1.3. Điểm số Hiện tại

```
  Chức năng (Features):     ████████░░  80% — Thiếu file upload, edit/delete, search
  Bảo mật (Security):       ██████░░░░  60% — Thiếu rate limit, E2E encryption, CORS strict
  Hiệu năng (Performance):  ███████░░░  70% — Single instance, chưa cache
  Vận hành (Operations):     ████░░░░░░  40% — Thiếu monitoring, CI/CD, logging
  Kiểm thử (Testing):       ███░░░░░░░  30% — Chỉ có manual test, chưa có JUnit
  ──────────────────────────────────────────
  TỔNG THỂ:                  ██████░░░░  56%
```

---

## 2. LỘ TRÌNH NÂNG CẤP TỔNG QUAN

```
  v1.0 (Hiện tại)                    v1.5                      v2.0                     v3.0                    v4.0
  ─────────────────── ▶ ──────────────────── ▶ ──────────────────── ▶ ──────────────────── ▶ ─────────────────────
  MVP+ Chat App          Hoàn thiện Core        Tính năng Nâng cao     Enterprise & Scale     Production-Ready
  │                      │                      │                      │                      │
  • Chat 1-1, Nhóm       • Health Check         • File Upload (MinIO)  • Kubernetes           • CI/CD Pipeline
  • WebSocket             • Rate Limiting        • Video/Voice Call     • Horizontal Scale     • Blue-Green Deploy
  • JWT Auth              • API Gateway          • Read Receipts        • Service Mesh         • Monitoring Stack
  • Typing, Presence      • Unit Tests           • Emoji Reactions      • Full-text Search     • Disaster Recovery
  • Media URL             • Pagination           • Message Edit/Delete  • E2E Encryption       • Performance Tuning
  │                      │                      • AR Meme Matching     │                      │
  Thời gian: Đã xong     ~2-3 tuần              ~4-6 tuần              ~6-8 tuần              ~4-6 tuần
```

---

## 3. PHASE 1: HOÀN THIỆN CORE (v1.5)

**Thời gian dự kiến:** 2-3 tuần  
**Mục tiêu:** Khắc phục các điểm yếu nghiêm trọng nhất, chuẩn bị nền tảng cho scale.

### 3.1. Spring Actuator — Health Check & Monitoring

**Vấn đề:** Không có endpoint để kiểm tra trạng thái service, Docker/K8s không thể health check.

**Giải pháp:**

Thêm dependency vào `pom.xml` của cả 3 service:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Cấu hình `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: always
      show-components: always
  health:
    redis:
      enabled: true
    cassandra:
      enabled: true
    db:
      enabled: true
```

**Kết quả:** Endpoint `/actuator/health` trả về chi tiết trạng thái DB, Redis, Cassandra.

---

### 3.2. Rate Limiting — Chống Spam & DDoS

**Vấn đề:** Không giới hạn số request, user có thể spam hàng nghìn tin nhắn/giây.

**Giải pháp:** Sử dụng Bucket4j + Redis để rate limit phân tán.

Thêm dependency:
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.7.0</version>
</dependency>
```

Tạo `RateLimitFilter.java`:
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    // Giới hạn mỗi user: 60 request/phút cho REST, 30 tin nhắn/phút cho WS
    private static final int REST_LIMIT = 60;
    private static final int MESSAGE_LIMIT = 30;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response,
                                     FilterChain chain) {
        String userId = extractUserId(request);
        Bucket bucket = resolveBucket(userId);
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
        }
    }
}
```

**Cấu hình giới hạn:**

| Loại | Giới hạn | Mô tả |
| :--- | :---: | :--- |
| REST API (auth) | 10 req/phút | Đăng ký, đăng nhập |
| REST API (chat) | 60 req/phút | Tạo conversation, lấy lịch sử |
| WebSocket message | 30 msg/phút | Gửi tin nhắn qua WS |
| WebSocket connect | 5 lần/phút | Kết nối WS mới |

---

### 3.3. API Gateway — Unified Entry Point

**Vấn đề:** Client phải biết 3 port khác nhau (8081, 8082, 8083).

**Giải pháp:** Thêm Spring Cloud Gateway làm single entry point.

```
Client ──── Port 80 ───> [API Gateway]
                              │
                              ├── /api/auth/**  ──> auth-service:8081
                              ├── /api/chat/**  ──> chat-service:8082
                              └── /ws/**        ──> ws-gateway:8083 (WS upgrade)
```

Tạo service mới `api-gateway/`:
```yaml
# application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://auth-service:8081
          predicates:
            - Path=/api/auth/**, /api/users
        - id: chat-service
          uri: http://chat-service:8082
          predicates:
            - Path=/api/conversations/**, /api/messages/**
        - id: ws-gateway
          uri: ws://ws-gateway:8083
          predicates:
            - Path=/ws/**
```

---

### 3.4. Unit Test (JUnit 5 + Mockito)

**Vấn đề:** Không có test tự động, mọi thay đổi code đều có rủi ro regression.

**Giải pháp:** Viết JUnit test cho mỗi service.

**Danh sách file test cần tạo:**

| Service | File Test | Số Test Cases | Mô tả |
| :--- | :--- | :---: | :--- |
| auth-service | `JwtServiceTest.java` | 5 | Generate, parse, expired, invalid signature |
| auth-service | `AuthServiceTest.java` | 6 | Register, login, refresh, duplicate check |
| chat-service | `ChatServiceTest.java` | 8 | Create direct/group, send message, history, membership |
| ws-gateway | `SessionRegistryTest.java` | 4 | Register, unregister, online users |
| ws-gateway | `ChatWebSocketHandlerTest.java` | 5 | CHAT frame, TYPING frame, connection, disconnect |

Ví dụ `JwtServiceTest.java`:
```java
@SpringBootTest
class JwtServiceTest {
    
    @Autowired
    private JwtService jwtService;
    
    @Test
    void generateAccessToken_shouldContainCorrectClaims() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        
        String token = jwtService.generateAccessToken(user);
        
        Jws<Claims> jws = jwtService.parse(token);
        assertEquals(user.getId().toString(), jws.getPayload().getSubject());
        assertEquals("testuser", jws.getPayload().get("username"));
        assertEquals("access", jws.getPayload().get("typ"));
    }
    
    @Test
    void parse_expiredToken_shouldThrowException() {
        // Token with 0ms TTL
        assertThrows(ExpiredJwtException.class, () -> {
            jwtService.parse(expiredToken);
        });
    }
}
```

---

### 3.5. Infinite Scroll — Pagination UI

**Vấn đề:** Chỉ load 50 tin nhắn gần nhất, không thể xem lịch sử cũ hơn.

**Giải pháp:**

Backend đã hỗ trợ phân trang Cassandra (`CassandraPageRequest`). Cần:

1. Thêm API parameter `?before=<messageId>` để load tin nhắn cũ hơn
2. Frontend: Infinite scroll khi user cuộn lên đầu danh sách

```java
// MessageController.java — Thêm cursor-based pagination
@GetMapping
public List<MessageResponse> getHistory(
        @RequestParam UUID conversationId,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(required = false) UUID before) {  // cursor
    return chatService.getHistory(userId, conversationId, before, size);
}
```

---

## 4. PHASE 2: TÍNH NĂNG NÂNG CAO (v2.0)

**Thời gian dự kiến:** 4-6 tuần  
**Mục tiêu:** Bổ sung tính năng người dùng thực sự cần, nâng trải nghiệm UX.

### 4.1. File Upload thực (MinIO Integration)

**Hiện tại:** Chỉ hỗ trợ paste URL ảnh bên ngoài.  
**Nâng cấp:** Upload file trực tiếp lên MinIO (S3-compatible), trả về URL tải về.

```
Client ──── multipart/form-data ───> chat-service ───> MinIO (S3 bucket)
                                          │
                                          └── Trả về URL: http://minio:9000/chat-files/abc.jpg
```

Tạo `FileUploadService.java`:
```java
@Service
public class FileUploadService {
    
    private final MinioClient minioClient;
    
    public String uploadFile(MultipartFile file) {
        String objectName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket("chat-files")
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket("chat-files")
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(7, TimeUnit.DAYS)
                        .build());
    }
}
```

**API mới:**
```
POST /api/files/upload
Content-Type: multipart/form-data
Body: file=<binary>
Response: { "url": "http://...", "fileName": "...", "fileSize": 12345 }
```

**Giới hạn upload:**

| Loại file | Kích thước tối đa | Định dạng |
| :--- | :---: | :--- |
| Ảnh | 10 MB | JPG, PNG, GIF, WebP |
| Video | 50 MB | MP4, WebM |
| Tài liệu | 20 MB | PDF, DOC, DOCX |
| Khác | 5 MB | ZIP, RAR |

---

### 4.2. Message Edit & Delete

**API mới:**

```
PUT    /api/messages/{messageId}    { content: "nội dung mới" }
DELETE /api/messages/{messageId}
```

**Logic:**
- Chỉ người gửi mới được sửa/xóa tin nhắn của mình
- Sửa: Cập nhật `content`, thêm field `edited_at` timestamp, broadcast `MESSAGE_EDITED` frame
- Xóa: Soft delete (đánh dấu `status = "DELETED"`), broadcast `MESSAGE_DELETED` frame

**Cassandra schema update:**
```cql
ALTER TABLE chat_system.messages ADD edited_at timestamp;
```

**WebSocket frames mới:**
```json
{ "type": "MESSAGE_EDITED", "messageId": "...", "newContent": "...", "editedAt": "..." }
{ "type": "MESSAGE_DELETED", "messageId": "...", "conversationId": "..." }
```

---

### 4.3. Read Receipts (Đã xem ✓✓ xanh)

**Luồng:**
1. Bob mở cuộc chat → Client gửi `READ_RECEIPT` frame chứa `conversationId` + `lastSeenMessageId`
2. ws-gateway forward tới chat-service → cập nhật `status = "READ"` trong Cassandra
3. Publish event Redis → Alice nhận `READ_RECEIPT` frame → UI đổi ✓✓ thành xanh

**Cassandra table mới:**
```cql
CREATE TABLE chat_system.read_receipts (
    conversation_id uuid,
    user_id uuid,
    last_seen_message_id timeuuid,
    seen_at timestamp,
    PRIMARY KEY (conversation_id, user_id)
);
```

**WebSocket frame:**
```json
// Client → Server
{ "type": "READ_RECEIPT", "conversationId": "...", "lastSeenMessageId": "..." }

// Server → Sender
{ "type": "READ_RECEIPT", "conversationId": "...", "userId": "...", "lastSeenMessageId": "..." }
```

---

### 4.4. Emoji Reactions

**Cho phép thả cảm xúc vào tin nhắn** (❤️ 👍 😂 😮 😢 😡)

**Cassandra table mới:**
```cql
CREATE TABLE chat_system.reactions (
    conversation_id uuid,
    message_id timeuuid,
    user_id uuid,
    emoji text,
    reacted_at timestamp,
    PRIMARY KEY ((conversation_id, message_id), user_id)
);
```

**API:**
```
POST   /api/messages/{messageId}/reactions   { emoji: "❤️" }
DELETE /api/messages/{messageId}/reactions
```

**WebSocket frame:**
```json
{ "type": "REACTION", "conversationId":"...", "messageId":"...", "userId":"...", "emoji":"❤️", "action":"ADD" }
```

---

### 4.5. Video & Voice Call (WebRTC)

**Kiến trúc:**
```
Alice ── Signaling (WebSocket) ──> ws-gateway ── Signaling ──> Bob
Alice ←─── WebRTC P2P Media Stream (ICE/STUN/TURN) ───────→ Bob
```

**Thành phần mới:**

| Thành phần | Vai trò |
| :--- | :--- |
| `TURN Server` (coturn) | Relay media khi P2P fail (NAT traversal) |
| `Signaling frames` | CALL_OFFER, CALL_ANSWER, ICE_CANDIDATE, CALL_HANGUP |

**WebSocket frames mới:**
```json
{ "type": "CALL_OFFER", "targetUserId": "...", "sdp": "...", "callType": "VIDEO" }
{ "type": "CALL_ANSWER", "targetUserId": "...", "sdp": "..." }
{ "type": "ICE_CANDIDATE", "targetUserId": "...", "candidate": "..." }
{ "type": "CALL_HANGUP", "targetUserId": "..." }
```

---

### 4.6. Nhận diện khuôn mặt & cảm xúc đối chiếu Meme thời gian thực (AR Meme Matching)

**Luồng hoạt động:**
1. Khi người dùng bật camera, giao diện sử dụng HTML5 MediaDevices API để hiển thị luồng video của chính họ.
2. Thư viện client-side (`face-api.js` hoặc TensorFlow.js) chạy trực tiếp trên trình duyệt của người dùng để phát hiện khuôn mặt và nhận diện cảm xúc thời gian thực (Vui, Buồn, Giận dữ, Ngạc nhiên, Sợ hãi...).
3. Ứng dụng client truy cập kho lưu trữ hình ảnh Meme trên MinIO (S3-compatible) đã được gán sẵn các nhãn cảm xúc tương ứng trong cơ sở dữ liệu.
4. Client đối chiếu cảm xúc trên khuôn mặt người dùng với các nhãn Meme và tìm ra Meme có sắc thái phù hợp nhất.
5. Giao diện chèn ảnh Meme song song hoặc đè trực tiếp lên video thời gian thực dưới dạng AR Overlay/Picture-in-Picture để người quay nhìn rõ mặt mình đang giống với Meme nào nhất.

**API mới:**
- **GET** `/api/memes/search?tag=HAPPY` -> Trả về danh sách hình ảnh Meme tương ứng với nhãn cảm xúc.
  ```json
  [
    { "id": "meme-123", "url": "http://minio:9000/memes/happy-doge.jpg", "emotion": "HAPPY" }
  ]
  ```

---

## 5. PHASE 3: ENTERPRISE & SCALE (v3.0)

**Thời gian dự kiến:** 6-8 tuần  
**Mục tiêu:** Scale hệ thống cho hàng nghìn người dùng đồng thời, bảo mật enterprise-grade.

### 5.1. Kubernetes Deployment

**Chuyển từ Docker Compose sang Kubernetes:**

```
                        ┌──────────────────────────────┐
                        │    Kubernetes Cluster (K8s)   │
                        │                              │
                        │  ┌─── Ingress Controller ──┐ │
                        │  │   (nginx / traefik)      │ │
                        │  └─────────┬────────────────┘ │
                        │            │                  │
                        │  ┌─────────▼────────────────┐ │
                        │  │    API Gateway (3 pods)   │ │
                        │  └────┬──────┬──────┬───────┘ │
                        │       │      │      │         │
                        │  ┌────▼──┐ ┌─▼───┐ ┌▼──────┐ │
                        │  │ auth  │ │chat │ │ws-gw  │ │
                        │  │(3pod) │ │(3pod)│ │(5pod) │ │
                        │  └───┬───┘ └──┬──┘ └───┬───┘ │
                        │      │        │        │      │
                        │  ┌───▼────────▼────────▼───┐  │
                        │  │ PostgreSQL  │ Cassandra  │  │
                        │  │  (3 nodes)  │ (3 nodes)  │  │
                        │  │     Redis Cluster (6)    │  │
                        │  └─────────────────────────┘  │
                        └──────────────────────────────┘
```

**Helm Chart structure:**
```
helm/chat-system/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── auth-deployment.yaml
│   ├── auth-service.yaml
│   ├── chat-deployment.yaml
│   ├── chat-service.yaml
│   ├── ws-gateway-deployment.yaml
│   ├── ws-gateway-service.yaml
│   ├── ingress.yaml
│   └── configmap.yaml
```

**Autoscaling:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ws-gateway-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ws-gateway
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Pods
      pods:
        metric:
          name: websocket_connections
        target:
          type: AverageValue
          averageValue: "1000"   # Scale khi mỗi pod > 1000 WS connections
```

---

### 5.2. Horizontal Scale ws-gateway

**Vấn đề hiện tại:** Single ws-gateway instance = single point of failure.

**Giải pháp:** Multiple ws-gateway instances + Redis Pub/Sub coordination.

```
                Load Balancer (sticky session)
                    │           │           │
              ┌─────▼──┐  ┌────▼───┐  ┌────▼───┐
              │ gw-1   │  │ gw-2   │  │ gw-3   │
              │ Alice   │  │ Bob    │  │ Charlie │
              └────┬───┘  └────┬───┘  └────┬───┘
                   │           │           │
                   └───────────┼───────────┘
                          Redis Pub/Sub
                     channel: chat.messages
```

**Cách hoạt động:**
1. Alice kết nối WS tới `gw-1`, Bob kết nối tới `gw-2`
2. Alice gửi tin nhắn → `gw-1` forward REST tới chat-service → persist + publish Redis
3. **Tất cả gateway instances** subscribe Redis channel → nhận event
4. `gw-2` kiểm tra "Bob có trong SessionRegistry không?" → Có → push xuống Bob

**Code đã sẵn sàng!** Kiến trúc Redis Pub/Sub hiện tại đã hỗ trợ multi-gateway. Chỉ cần:
- Thêm sticky session vào Load Balancer
- Scale `docker compose up --scale ws-gateway=3`

---

### 5.3. Full-text Search (Elasticsearch)

**Tìm kiếm tin nhắn theo nội dung:**

```
chat-service ── persist message ──> Cassandra
                                ──> Elasticsearch (async index)

Client ── GET /api/messages/search?q=hello ──> chat-service ──> Elasticsearch
```

Thêm service mới vào `docker-compose.yml`:
```yaml
elasticsearch:
  image: elasticsearch:8.12.0
  environment:
    - discovery.type=single-node
    - ES_JAVA_OPTS=-Xms512m -Xmx512m
  ports:
    - "9200:9200"
```

---

### 5.4. End-to-End Encryption (E2EE)

**Mô hình:** Signal Protocol (Double Ratchet Algorithm)

```
Alice                                              Bob
  │                                                  │
  │ ── Generate Key Pair (public + private) ──────>  │
  │ <── Bob's Public Key ─────────────────────────   │
  │                                                  │
  │ Encrypt(message, sharedSecret) ──> Server ──>    │
  │                                    (encrypted)   │
  │                                                  │
  │                            Decrypt(cipher, key) ─┤
```

**Server chỉ lưu trữ ciphertext** → Không thể đọc nội dung tin nhắn.

---

## 6. PHASE 4: PRODUCTION-READY (v4.0)

**Thời gian dự kiến:** 4-6 tuần  
**Mục tiêu:** CI/CD, monitoring, disaster recovery — sẵn sàng vận hành production thực tế.

### 6.1. CI/CD Pipeline (GitHub Actions)

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Run Unit Tests (auth-service)
        run: cd auth-service && mvn test
      - name: Run Unit Tests (chat-service)
        run: cd chat-service && mvn test
      - name: Run Unit Tests (ws-gateway)
        run: cd ws-gateway && mvn test

  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - name: Build Docker Images
        run: docker compose build
      - name: Push to Registry
        run: |
          docker tag chat-system-auth-service $REGISTRY/auth-service:$GITHUB_SHA
          docker push $REGISTRY/auth-service:$GITHUB_SHA

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Staging
        run: kubectl apply -f k8s/staging/

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production    # Manual approval required
    steps:
      - name: Blue-Green Deploy
        run: kubectl apply -f k8s/production/
```

---

### 6.2. Monitoring Stack (Prometheus + Grafana)

```
  ┌─────────────┐     ┌──────────────┐     ┌────────────┐
  │ Spring Boot │     │  Prometheus  │     │  Grafana   │
  │ /actuator/  │ ──> │  (scrape     │ ──> │ (dashboard │
  │  prometheus │     │   metrics)   │     │  & alerts) │
  └─────────────┘     └──────────────┘     └────────────┘
```

**Dashboard Grafana bao gồm:**

| Panel | Metrics | Mô tả |
| :--- | :--- | :--- |
| Active WS Connections | `websocket_sessions_active` | Số kết nối WebSocket đang mở |
| Messages/sec | `chat_messages_sent_total` rate | Tốc độ gửi tin nhắn |
| API Latency (p95) | `http_server_requests_seconds` | Độ trễ API percentile 95 |
| Error Rate | `http_server_requests{status=5xx}` rate | Tỷ lệ lỗi server |
| JVM Memory | `jvm_memory_used_bytes` | Bộ nhớ Java sử dụng |
| DB Connection Pool | `hikaricp_connections_active` | Số kết nối DB đang dùng |
| Redis Operations | `redis_commands_processed_total` | Số lệnh Redis xử lý |
| Cassandra Latency | `cassandra_request_latency` | Độ trễ truy vấn Cassandra |

**Alerting Rules:**
```yaml
# prometheus/alert_rules.yml
groups:
  - name: chat-system-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Error rate > 10% trong 5 phút"
          
      - alert: HighWSConnections
        expr: websocket_sessions_active > 5000
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Số WS connections vượt 5000, cân nhắc scale"
```

---

### 6.3. Centralized Logging (ELK Stack)

```
  ┌─────────────┐     ┌──────────────┐     ┌───────────┐     ┌──────────┐
  │ Spring Boot │ ──> │  Filebeat    │ ──> │ Logstash  │ ──> │ Elastic  │
  │ (JSON logs) │     │ (collect)    │     │ (parse)   │     │ Search   │
  └─────────────┘     └──────────────┘     └───────────┘     └────┬─────┘
                                                                   │
                                                              ┌────▼─────┐
                                                              │  Kibana  │
                                                              │ (search  │
                                                              │  & view) │
                                                              └──────────┘
```

**Cấu hình structured logging:**
```yaml
# application.yml
logging:
  pattern:
    console: '{"timestamp":"%d","level":"%p","service":"${spring.application.name}","traceId":"%X{traceId}","message":"%m"}%n'
```

**Distributed Tracing** với Spring Cloud Sleuth + Zipkin:
- Mỗi request được gán `traceId` duy nhất
- Trace xuyên suốt: auth-service → chat-service → ws-gateway
- Zipkin UI hiển thị waterfall diagram cho mỗi request

---

### 6.4. Disaster Recovery & Backup

| Thành phần | Chiến lược Backup | Tần suất | Retention |
| :--- | :--- | :---: | :---: |
| PostgreSQL | `pg_dump` → S3 | Mỗi 6 giờ | 30 ngày |
| Cassandra | `nodetool snapshot` → S3 | Mỗi ngày | 14 ngày |
| Redis | RDB Snapshot | Mỗi giờ | 7 ngày |
| MinIO | Cross-region replication | Real-time | Vĩnh viễn |

---

## 7. NÂNG CẤP KIẾN TRÚC

### 7.1. Kiến trúc Hiện tại vs Đề xuất

| Thành phần | Hiện tại (v1.0) | Đề xuất (v3.0+) |
| :--- | :--- | :--- |
| Entry Point | 3 port riêng biệt | API Gateway (1 port) |
| Service Discovery | Hardcoded URL | Spring Cloud Eureka / K8s DNS |
| Config Management | `application.yml` trong mỗi service | Spring Cloud Config Server |
| Load Balancing | Không có | Nginx / K8s Ingress |
| Circuit Breaker | Không có | Resilience4j |
| Message Queue | Redis Pub/Sub (fire-and-forget) | Apache Kafka (durable, replay) |
| Caching | Không có | Redis Cache (conversation list, user profile) |
| Session Store | In-memory Map | Redis Session Store |

### 7.2. Chuyển từ Redis Pub/Sub sang Apache Kafka

**Lý do:** Redis Pub/Sub là fire-and-forget (mất message nếu subscriber offline). Kafka đảm bảo:
- **Durability:** Message không bị mất
- **Replay:** Có thể đọc lại message cũ
- **Consumer Groups:** Nhiều instance cùng đọc mà không bị trùng

```yaml
# docker-compose.yml — Thêm Kafka
kafka:
  image: confluentinc/cp-kafka:7.5.0
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
  ports:
    - "9092:9092"
```

**Topics:**

| Topic | Producer | Consumer | Mô tả |
| :--- | :--- | :--- | :--- |
| `chat.messages` | chat-service | ws-gateway (consumer group) | Tin nhắn mới |
| `chat.presence` | ws-gateway | ws-gateway (broadcast) | Online/Offline |
| `chat.notifications` | chat-service | notification-service | Push notification |

---

## 8. NÂNG CẤP BẢO MẬT

| # | Hạng mục | Hiện tại | Đề xuất | Mức độ |
| :---: | :--- | :--- | :--- | :---: |
| 1 | HTTPS | Không (HTTP plain) | TLS 1.3 + Let's Encrypt | Nghiêm trọng |
| 2 | CORS | Mở rộng (`*`) | Whitelist domain cụ thể | Cao |
| 3 | Rate Limiting | Không có | Bucket4j + Redis | Nghiêm trọng |
| 4 | SQL Injection | Spring Data JPA (an toàn) | Giữ nguyên + OWASP audit | Đã tốt |
| 5 | XSS | Không sanitize | HTML sanitizer cho content tin nhắn | Cao |
| 6 | CSRF | Disabled (API stateless) | Giữ nguyên (hợp lý cho JWT) | Đã tốt |
| 7 | Password Policy | Không validate | Min 8 ký tự, uppercase, number, special | Trung bình |
| 8 | Account Lockout | Không có | Khóa 15 phút sau 5 lần sai mật khẩu | Cao |
| 9 | JWT Revocation | Không thể thu hồi | Redis blacklist cho revoked tokens | Trung bình |
| 10 | Input Validation | Cơ bản | Bean Validation (@NotBlank, @Size, @Email) | Trung bình |

**Ví dụ XSS Sanitizer:**
```java
@Component
public class ContentSanitizer {
    
    private final PolicyFactory policy = new HtmlPolicyBuilder()
            .allowElements("b", "i", "u", "br")
            .allowUrlProtocols("https")
            .toFactory();
    
    public String sanitize(String content) {
        return policy.sanitize(content);
    }
}
```

---

## 9. NÂNG CẤP HIỆU NĂNG

### 9.1. Redis Cache Layer

| Cache Key | TTL | Mô tả |
| :--- | :---: | :--- |
| `user:{userId}` | 30 phút | Profile user (tránh query DB mỗi request) |
| `conv:{userId}` | 5 phút | Danh sách conversation của user |
| `members:{convId}` | 10 phút | Danh sách thành viên conversation |
| `online:users` | Real-time | Set userId đang online (đã có) |

**Cache Strategy:** Cache-Aside Pattern
```java
public UserProfile getProfile(UUID userId) {
    String key = "user:" + userId;
    UserProfile cached = redisTemplate.opsForValue().get(key);
    if (cached != null) return cached;
    
    UserProfile profile = userRepository.findById(userId)...;
    redisTemplate.opsForValue().set(key, profile, Duration.ofMinutes(30));
    return profile;
}
```

### 9.2. Connection Pooling Optimization

```yaml
# HikariCP (PostgreSQL)
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # Tăng từ default 10
      minimum-idle: 5
      idle-timeout: 300000        # 5 phút
      connection-timeout: 20000   # 20 giây

# Cassandra Driver
datastax-java-driver:
  advanced:
    connection:
      max-requests-per-connection: 1024
      pool:
        local:
          size: 4
        remote:
          size: 2
```

### 9.3. WebSocket Compression

```java
// WebSocketConfig.java — Bật permessage-deflate
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(handler, "/ws")
            .addInterceptors(interceptor)
            .setAllowedOrigins("*")
            .withSockJS()                    // Fallback cho browser cũ
            .setWebSocketEnabled(true)
            .setStreamBytesLimit(512 * 1024); // 512KB
}
```

---

## 10. NÂNG CẤP DEVOPS & VẬN HÀNH

### 10.1. Docker Image Optimization

| Cải tiến | Trước | Sau | Giảm |
| :--- | :---: | :---: | :---: |
| Base image | `eclipse-temurin:21-jre-alpine` | `eclipse-temurin:21-jre-alpine` (giữ) | 0% |
| Layer caching | Build lại toàn bộ mỗi lần | Tách layer `dependencies` và `application` | -50% build time |
| Multi-stage build | Đã có | Giữ nguyên | Đã tốt |
| .dockerignore | Có | Thêm `*.md`, `test/`, `.git/` | -10% context |

### 10.2. Git Branching Strategy

```
main ────────────────────────────────────────────> Production
  │                                                  ▲
  └── develop ──────────────────────────────────>  Staging
        │                                            ▲
        ├── feature/file-upload ──────────────>   │
        ├── feature/video-call ───────────────>   │
        ├── feature/emoji-reactions ──────────>   │
        └── hotfix/jwt-expiry-fix ─────────────────>
```

---

## 11. ƯỚC TÍNH NGUỒN LỰC

### 11.1. Nhân sự

| Phase | Thời gian | Backend Dev | Frontend Dev | DevOps | Tổng |
| :--- | :---: | :---: | :---: | :---: | :---: |
| Phase 1 (v1.5) | 2-3 tuần | 1 | 0 | 1 | 2 |
| Phase 2 (v2.0) | 4-6 tuần | 2 | 1 | 0 | 3 |
| Phase 3 (v3.0) | 6-8 tuần | 2 | 1 | 1 | 4 |
| Phase 4 (v4.0) | 4-6 tuần | 1 | 0 | 2 | 3 |

### 11.2. Hạ tầng Production (Ước tính)

| Thành phần | Spec | Số lượng | Chi phí/tháng (AWS) |
| :--- | :--- | :---: | :---: |
| EKS Cluster | 3 nodes t3.medium | 1 | ~$200 |
| RDS PostgreSQL | db.t3.medium, Multi-AZ | 1 | ~$150 |
| ElastiCache Redis | cache.t3.medium | 1 | ~$70 |
| Cassandra (EC2) | r5.large (16GB RAM) | 3 | ~$300 |
| ALB (Load Balancer) | Application LB | 1 | ~$30 |
| S3 (File storage) | Standard | 100GB | ~$3 |
| CloudWatch/Grafana | Monitoring | 1 | ~$30 |
| **TỔNG** | | | **~$783/tháng** |

---

## 12. BẢNG SO SÁNH TRƯỚC & SAU

| Chỉ số | v1.0 (Hiện tại) | v4.0 (Mục tiêu) |
| :--- | :---: | :---: |
| **Tính năng** | 15 | 31+ |
| **Concurrent Users** | ~100 | 10,000+ |
| **WS Connections** | 1 instance (~500 max) | Multi-instance (50,000+) |
| **Message Throughput** | ~100 msg/sec | 10,000+ msg/sec |
| **Uptime SLA** | Không cam kết | 99.9% |
| **Deploy Time** | Thủ công (~5 phút) | Tự động CI/CD (~2 phút) |
| **Recovery Time** | Thủ công | Auto-healing (< 30 giây) |
| **Monitoring** | Không có | Full-stack (Prometheus + Grafana) |
| **Test Coverage** | 0% (manual) | 80%+ (JUnit + Integration) |
| **Security Score** | 60% | 95%+ |

### Điểm số Sau Nâng cấp (Mục tiêu v4.0)

```
  Chức năng (Features):     ██████████  100% — Đầy đủ tính năng enterprise
  Bảo mật (Security):       █████████░  95%  — E2EE, Rate Limit, HTTPS, Audit
  Hiệu năng (Performance):  █████████░  95%  — Multi-instance, Cache, Kafka
  Vận hành (Operations):     █████████░  90%  — CI/CD, Monitoring, DR
  Kiểm thử (Testing):       ████████░░  80%  — JUnit, Integration, E2E
  ──────────────────────────────────────────────
  TỔNG THỂ:                  █████████░  92%  (tăng từ 56%)
```

---

*Đề xuất được lập ngày 18/08/2026.*  
*Ưu tiên triển khai: Phase 1 → Phase 2 → Phase 3 → Phase 4*
