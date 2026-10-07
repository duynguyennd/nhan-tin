# Real-time Chat Application — Microservices Architecture

Hệ thống nhắn tin thời gian thực (Real-time Chat) xây dựng theo kiến trúc **Mini-Microservices** (Java Spring Boot 3.3, WebSocket, PostgreSQL, Cassandra, Redis Pub/Sub).

---

## 🏛️ 1. Kiến trúc Hệ thống

```
Client ──HTTP──> auth-service  (8081)  ──> PostgreSQL (authdb)
Client ──WSS───> ws-gateway    (8083)  ──REST(Bearer JWT)──> chat-service (8082)
                      ▲                                        │
                      │                                        ├──> PostgreSQL (chatdb: conversations, members)
                      │                                        ├──> Cassandra (chat_system.messages)
                      │                                        └──> Redis Pub/Sub channel "chat.messages"
                      └──────── push MESSAGE frame ────────────┘
                    (Gateway subscribe Redis & push xuống Client)
```

| Thành phần | Port | Công nghệ | Vai trò |
|---|:---:|---|---|
| **auth-service** | `8081` | Spring Boot 3.3, JPA, JJWT | Xác thực, đăng ký/đăng nhập, danh sách user (`/api/users`) |
| **chat-service** | `8082` | Spring Boot 3.3, Cassandra, JPA, Redis | Tạo chat 1-1 & nhóm chat, lưu tin nhắn, publish Redis event |
| **ws-gateway** | `8083` | Spring Boot 3.3, WebSocket, Redis Sub | Quản lý kết nối WS, push tin nhắn & trạng thái Online/Offline |
| **PostgreSQL 16** | `5432` | RDBMS | Dữ liệu tài khoản (`authdb`), thông tin hội thoại (`chatdb`) |
| **Cassandra 5.0** | `9042` | NoSQL | Lịch sử tin nhắn ghi tốc độ cao (`chat_system.messages`) |
| **Redis 7** | `6379` | Pub/Sub + Cache | Bus thông điệp truyền tin nhắn bất đồng bộ |
| **MinIO** | `9001` | Object Storage | Lưu trữ file / hình ảnh media |

---

## 🚀 2. Danh sách các Lệnh Chạy Hệ thống (System Run Commands)

### 2.1. Chạy toàn bộ ứng dụng bằng Docker Compose (Khuyên dùng)

```powershell
# Chạy & tự động build toàn bộ containers ở chế độ ngầm (Background/Detached)
docker compose up -d --build

# Xem log thời gian thực của toàn bộ hệ thống
docker compose logs -f

# Xem log của một service cụ thể (ví dụ: ws-gateway)
docker compose logs -f ws-gateway
```

### 2.2. Các lệnh Dừng / Quản lý Docker Containers

```powershell
# Xem trạng thái các container đang chạy
docker compose ps

# Dừng toàn bộ hệ thống (giữ lại dữ liệu database)
docker compose stop

# Khởi động lại toàn bộ hệ thống
docker compose restart

# Xóa toàn bộ container và hạ tầng
docker compose down

# Xóa toàn bộ container + làm sạch dữ liệu volume DB (Reset hoàn toàn)
docker compose down -v
```

### 2.3. Chạy từng Service ở chế độ Developer (Local Dev Mode)

Nếu muốn chạy hạ tầng DB trên Docker và chạy code Java trực tiếp trên IDE / Terminal:

```powershell
# Bước 1: Chỉ chạy hạ tầng Database & Redis
docker compose up -d postgres redis cassandra cassandra-init

# Bước 2: Chạy auth-service (Terminal 1)
cd auth-service
mvn spring-boot:run

# Bước 3: Chạy chat-service (Terminal 2)
cd chat-service
mvn spring-boot:run

# Bước 4: Chạy ws-gateway (Terminal 3)
cd ws-gateway
mvn spring-boot:run
```

---

## 💻 3. Lệnh Mở Giao diện Web Client UI (`demo-client`)

Chạy lệnh PowerShell để trình duyệt tự động mở giao diện Web Client:

```powershell
# Mở trực tiếp file index.html trên Trình duyệt mặc định
Start-Process "C:\Users\duy\Downloads\laptrinhjava\demo-client\index.html"
```
*(Gợi ý: Mở trên 2-3 tab trình duyệt khác nhau để test chat 1-1, chat nhóm và xem chấm trạng thái Trực tuyến / Ngoại tuyến)*.

---

## 🧪 4. Danh sách các Lệnh cURL Kiểm thử API (API Testing Commands)

### 4.1. Đăng ký tài khoản mới

```powershell
# Đăng ký tài khoản Alice
curl -X POST http://localhost:8081/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"username":"alice","email":"alice@mail.com","password":"secretpassword"}'

# Đăng ký tài khoản Bob
curl -X POST http://localhost:8081/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{"username":"bob","email":"bob@mail.com","password":"secretpassword"}'
```

### 4.2. Đăng nhập & Lấy Access Token

```powershell
curl -X POST http://localhost:8081/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"usernameOrEmail":"alice","password":"secretpassword"}'
```

### 4.3. Lấy danh sách tất cả người dùng

```powershell
curl -X GET http://localhost:8081/api/users
```

### 4.4. Tạo cuộc trò chuyện 1-1 (Direct Conversation)

```powershell
curl -X POST http://localhost:8082/api/conversations/direct `
  -H "Authorization: Bearer <ALICE_ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{"targetUserId":"<BOB_USER_ID>"}'
```

### 4.5. Tạo cuộc trò chuyện Nhóm (Group Chat)

```powershell
curl -X POST http://localhost:8082/api/conversations/group `
  -H "Authorization: Bearer <ALICE_ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{"name":"Nhóm Lập Trình Java","memberUserIds":["<BOB_USER_ID>","<CHARLIE_USER_ID>"]}'
```

### 4.6. Xem lịch sử tin nhắn

```powershell
curl -X GET "http://localhost:8082/api/messages?conversationId=<CONVERSATION_ID>&size=50" `
  -H "Authorization: Bearer <ALICE_ACCESS_TOKEN>"
```

### 4.7. Kiểm thử các tính năng AI Copilot & Smart Features

```powershell
# 1. Trò chuyện trực tiếp với Gemini AI Copilot
curl -X POST http://localhost:8082/api/ai/chat `
  -H "Authorization: Bearer <ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{"conversationId":"<CONVERSATION_ID>","prompt":"Giải thích cơ chế Redis Pub/Sub"}'

# 2. Tóm tắt cuộc trò chuyện & trích xuất việc cần làm (Action Items)
curl -X POST http://localhost:8082/api/ai/summarize `
  -H "Authorization: Bearer <ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{"conversationId":"<CONVERSATION_ID>"}'

# 3. Gợi ý phản hồi thông minh (Smart Replies)
curl -X POST http://localhost:8082/api/ai/smart-replies `
  -H "Authorization: Bearer <ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{"conversationId":"<CONVERSATION_ID>","lastMessage":"Tiến độ dự án đã xong chưa?"}'

# 4. Dịch thuật tin nhắn tức thì (Live Translate)
curl -X POST http://localhost:8082/api/ai/translate `
  -H "Authorization: Bearer <ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{"text":"Hello team, everything is running smoothly!","targetLang":"vi"}'

# 5. Hiệu chỉnh văn phong tin nhắn (AI Tone Polisher: PROFESSIONAL | POLITE | CONCISE | CASUAL)
curl -X POST http://localhost:8082/api/ai/rewrite `
  -H "Authorization: Bearer <ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{"text":"bạn kiểm tra lại báo cáo giùm mình","tone":"PROFESSIONAL"}'

# 6. Sinh ảnh nghệ thuật AI (/imagine: CYBERPUNK | 3D | ANIME | PIXEL_ART)
curl -X POST http://localhost:8082/api/ai/imagine `
  -H "Authorization: Bearer <ACCESS_TOKEN>" `
  -H "Content-Type: application/json" `
  -d '{"prompt":"Futuristic Hologram AI Space","style":"CYBERPUNK"}'
```

---

## 🗄️ 5. Lệnh Truy vấn Cơ sở Dữ liệu (Database Commands)

### 5.1. Truy vấn PostgreSQL (`authdb` và `chatdb`)

```powershell
# Truy cập PostgreSQL container bằng psql
docker exec -it chat-postgres psql -U chatuser -d authdb

# Xem danh sách bảng và dữ liệu tài khoản trong authdb:
authdb=> \dt
authdb=> SELECT id, username, email, created_at FROM users;

# Chuyển sang database chatdb để xem danh sách hội thoại:
authdb=> \c chatdb
chatdb=> SELECT * FROM conversations;
chatdb=> SELECT * FROM conversation_members;
chatdb=> \q
```

### 5.2. Truy vấn Cassandra (`chat_system.messages`)

```powershell
# Truy cập Cassandra cqlsh terminal
docker exec -it chat-cassandra cqlsh

# Xem danh sách tin nhắn lưu trữ trong Cassandra:
cqlsh> USE chat_system;
cqlsh:chat_system> SELECT conversation_id, bucket_id, message_id, sender_id, content, status FROM messages;
cqlsh:chat_system> exit;
```

### 5.3. Theo dõi Redis Event Bus Pub/Sub

```powershell
# Lắng nghe thời gian thực channel "chat.messages" trên Redis
docker exec -it chat-redis redis-cli SUBSCRIBE chat.messages
```

---

## 📡 6. Giao thức WebSocket Frames

| Hướng | Frame JSON | Ý nghĩa |
|---|---|---|
| Client ➔ GW | `{"type":"CHAT", "conversationId":"...", "content":"...", "mediaUrls":[]}` | Gửi tin nhắn |
| Client ➔ GW | `{"type":"TYPING", "conversationId":"...", "isTyping":true}` | Thông báo đang nhập |
| Client ➔ GW | `{"type":"PING"}` | Heartbeat duy trì kết nối |
| GW ➔ Client | `{"type":"CONNECTED", "userId":"..."}` | Xác nhận kết nối thành công |
| GW ➔ Client | `{"type":"ONLINE_USERS", "userIds":["..."]}` | Danh sách user đang online |
| GW ➔ Client | `{"type":"PRESENCE", "userId":"...", "status":"ONLINE"}` | Trạng thái Online/Offline |
| GW ➔ Client | `{"type":"ACK", "conversationId":"...", "messageId":"...", "status":"SENT"}` | Server xác nhận đã lưu |
| GW ➔ Client | `{"type":"MESSAGE", "messageId":"...", "senderId":"...", "content":"..."}` | Tin nhắn real-time tới |
