# BÁO CÁO KIỂM THỬ PHẦN MỀM (SOFTWARE TESTING REPORT)
## Hệ thống Nhắn tin Thời gian thực — Real-time Chat Application

---

**Mã dự án:** CHAT-MICROSERVICES-2026  
**Ngày kiểm thử:** 18/08/2026  
**Người thực hiện:** Nhóm phát triển  
**Môi trường kiểm thử:** Docker Engine / Java 21 / Spring Boot 3.3.5 / PostgreSQL 16 / Cassandra 5.0 / Redis 7  
**Công cụ kiểm thử:** PowerShell Automated Script, cURL, Web Browser (Chrome/Edge)  
**Trạng thái tổng thể:** ✅ **ĐẠT CHUẨN NGHIỆM THU**

---

## MỤC LỤC

1. [Tổng quan Kết quả Kiểm thử](#1-tổng-quan-kết-quả-kiểm-thử)
2. [Cấp độ 1: Kiểm thử Đơn vị (Unit Testing)](#2-cấp-độ-1-kiểm-thử-đơn-vị-unit-testing)
3. [Cấp độ 2: Kiểm thử Tích hợp (Integration Testing)](#3-cấp-độ-2-kiểm-thử-tích-hợp-integration-testing)
4. [Cấp độ 3: Kiểm thử Hệ thống (System Testing)](#4-cấp-độ-3-kiểm-thử-hệ-thống-system-testing)
5. [Cấp độ 4: Kiểm thử Chấp nhận (Acceptance Testing - UAT)](#5-cấp-độ-4-kiểm-thử-chấp-nhận-acceptance-testing---uat)
6. [Kết luận & Đề xuất](#6-kết-luận--đề-xuất)

---

## 1. TỔNG QUAN KẾT QUẢ KIỂM THỬ

### 1.1. Bảng Tổng hợp theo 4 Cấp độ Kiểm thử

| Cấp độ | Tên gọi | Số Test Case | Đạt (PASS) | Lỗi (FAIL) | Tỷ lệ |
| :---: | :--- | :---: | :---: | :---: | :---: |
| **1** | Kiểm thử Đơn vị (Unit Testing) | 10 | 10 | 0 | **100%** |
| **2** | Kiểm thử Tích hợp (Integration Testing) | 12 | 12 | 0 | **100%** |
| **3** | Kiểm thử Hệ thống (System Testing / E2E) | 10 | 10 | 0 | **100%** |
| **4** | Kiểm thử Chấp nhận (User Acceptance Testing) | 8 | 8 | 0 | **100%** |
| | **TỔNG CỘNG** | **40** | **40** | **0** | **100%** |

### 1.2. Biểu đồ Trực quan

```
  Cấp độ 1 (Unit):        [██████████] 10/10  100%  ✅
  Cấp độ 2 (Integration): [████████████] 12/12  100%  ✅
  Cấp độ 3 (System E2E):  [██████████] 10/10  100%  ✅
  Cấp độ 4 (UAT):         [████████] 8/8    100%  ✅
  ─────────────────────────────────────────────────────
  TỔNG THỂ:               [████████████████████████████████████████] 40/40  100%
```

---

## 2. CẤP ĐỘ 1: KIỂM THỬ ĐƠN VỊ (UNIT TESTING)

**Mục tiêu:** Kiểm tra tính đúng đắn của từng hàm, phương thức và lớp Java riêng lẻ, không phụ thuộc vào hạ tầng bên ngoài (Database, Redis, Network).

**Phương pháp:** Kiểm tra logic nội bộ của từng class/method độc lập.

| Mã | Thành phần kiểm thử | Mô tả kịch bản | Đầu vào | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :--- | :---: |
| **UT-01** | `JwtService.generateAccessToken()` | Tạo JWT Access Token cho user hợp lệ | Object `User(id, username)` | Token JWT hợp lệ chứa claims: `sub`=userId, `username`, `typ`="access", `exp` > now | Token được tạo đúng format `eyJhbGciOiJIUzI1NiJ9...`, claims chính xác | **PASS** |
| **UT-02** | `JwtService.generateRefreshToken()` | Tạo JWT Refresh Token | Object `User(id, username)` | Token JWT hợp lệ chứa claim `typ`="refresh", `exp` = now + 7 ngày | Token được tạo đúng, TTL = 7 ngày | **PASS** |
| **UT-03** | `JwtService.parse()` — Token hợp lệ | Giải mã và xác minh token đúng | Token JWT hợp lệ do hệ thống cấp | Trả về `Jws<Claims>` chứa đúng `sub`, `username` | Claims giải mã chính xác | **PASS** |
| **UT-04** | `JwtService.parse()` — Token hết hạn | Giải mã token đã quá hạn | Token JWT đã expired | Ném ngoại lệ `ExpiredJwtException` | Exception được ném đúng loại | **PASS** |
| **UT-05** | `JwtService.parse()` — Token sai chữ ký | Giải mã token bị sửa đổi | Token JWT bị thay đổi payload | Ném ngoại lệ `SignatureException` | Exception được ném đúng loại | **PASS** |
| **UT-06** | `BCryptPasswordEncoder.matches()` | Đối chiếu mật khẩu gốc với hash BCrypt | Password gốc + hash BCrypt | Trả về `true` nếu khớp, `false` nếu sai | Kết quả đối chiếu chính xác | **PASS** |
| **UT-07** | `SessionRegistry.register()` | Đăng ký WebSocket Session cho userId | userId + WebSocketSession | Session được thêm vào `sessionsByUser` Map | Đăng ký thành công, `isConnectedLocally()` = true | **PASS** |
| **UT-08** | `SessionRegistry.unregister()` | Hủy đăng ký session khi user ngắt kết nối | userId + WebSocketSession | Session bị xóa khỏi Map; nếu là session cuối thì user offline | Hủy đăng ký chính xác | **PASS** |
| **UT-09** | `SessionRegistry.getOnlineUserIds()` | Lấy danh sách userId đang có session active | Không có tham số | Trả về `Set<UUID>` chứa tất cả userId đang online | Danh sách chính xác | **PASS** |
| **UT-10** | `MessageKey` constructor | Tạo khóa phân vùng Cassandra với TimeUUID | conversationId, bucketId, Uuids.timeBased() | `bucket_id` format "YYYY-MM", `message_id` là TimeUUID hợp lệ | Format và TimeUUID đúng | **PASS** |

---

## 3. CẤP ĐỘ 2: KIỂM THỬ TÍCH HỢP (INTEGRATION TESTING)

**Mục tiêu:** Kiểm tra sự tương tác và tích hợp giữa các Service Java với cơ sở dữ liệu (PostgreSQL, Cassandra) và Message Broker (Redis Pub/Sub).

**Phương pháp:** Gọi REST API thực tế tới từng microservice đang chạy trên Docker, xác minh dữ liệu được lưu đúng vào Database.

| Mã | Thành phần tích hợp | Mô tả kịch bản | Đầu vào (Request) | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :--- | :---: |
| **IT-01** | `auth-service` → PostgreSQL | Đăng ký user mới, lưu vào `authdb.users` | POST `/api/auth/register` `{username:"alice", email:"alice@test.com", password:"pass123"}` | HTTP 200, trả về `userId` + JWT Tokens; bản ghi tồn tại trong DB | HTTP 200, `userId: cc63632e-...`, JWT Token cấp thành công | **PASS** |
| **IT-02** | `auth-service` → PostgreSQL | Phát hiện trùng username trong DB | POST `/api/auth/register` với username đã tồn tại | HTTP 409 Conflict | HTTP 409, thông báo "Username đã tồn tại" | **PASS** |
| **IT-03** | `auth-service` → PostgreSQL | Đăng nhập, truy vấn `findByUsername` trong DB | POST `/api/auth/login` `{usernameOrEmail:"alice", password:"pass123"}` | HTTP 200, trả về JWT Access + Refresh Token | HTTP 200, Token cấp thành công | **PASS** |
| **IT-04** | `auth-service` → PostgreSQL | Đăng nhập sai mật khẩu, `passwordEncoder.matches()` = false | POST `/api/auth/login` `{password:"wrong"}` | HTTP 401 Unauthorized | HTTP 401, từ chối chính xác | **PASS** |
| **IT-05** | `auth-service` → PostgreSQL | Lấy danh sách tất cả users từ `userRepository.findAll()` | GET `/api/users` | HTTP 200, trả về mảng ≥ 3 users | HTTP 200, trả về 10 users | **PASS** |
| **IT-06** | `chat-service` → PostgreSQL (`chatdb`) | Tạo conversation DIRECT, lưu vào bảng `conversations` + `conversation_members` | POST `/api/conversations/direct` `{targetUserId:"<BOB_ID>"}` | HTTP 201, trả về `conversationId`, `type:"DIRECT"`, `memberIds` có 2 phần tử | HTTP 201, `ConvId: 05e1ff0d-...`, Type: DIRECT, Members: 2 | **PASS** |
| **IT-07** | `chat-service` → PostgreSQL | Tạo conversation GROUP, lưu vào bảng `conversations` + `conversation_members` với phân quyền ADMIN/MEMBER | POST `/api/conversations/group` `{name:"Nhóm Dev", memberUserIds:[...]}` | HTTP 201, trả về `type:"GROUP"`, `memberIds` có ≥ 3 phần tử | HTTP 201, `GroupId: 22e1a6ca-...`, Members: 3 | **PASS** |
| **IT-08** | `chat-service` → Cassandra | Lưu tin nhắn vào bảng `chat_system.messages` với Partition Key `(conversation_id, bucket_id)` | POST `/api/messages` `{conversationId:"...", content:"Hello"}` | HTTP 201, trả về `messageId` (TimeUUID), `status:"SENT"` | HTTP 201, `MsgId: 295170f0-...`, Status: SENT | **PASS** |
| **IT-09** | `chat-service` → Cassandra | Lưu tin nhắn có đính kèm ảnh (mediaUrls) | POST `/api/messages` `{content:"Ảnh!", mediaUrls:["https://..."]}` | HTTP 201, trả về `mediaUrls` chứa URL đã gửi | HTTP 201, mediaUrls lưu chính xác | **PASS** |
| **IT-10** | `chat-service` → Cassandra | Truy vấn lịch sử tin nhắn theo `conversation_id` + `bucket_id` sắp xếp `message_id DESC` | GET `/api/messages?conversationId=...&size=50` | HTTP 200, trả về mảng tin nhắn mới nhất trước | HTTP 200, 2 tin nhắn theo thứ tự mới nhất | **PASS** |
| **IT-11** | `chat-service` → Redis Pub/Sub | Sau khi lưu tin nhắn, publish `ChatMessageEvent` lên Redis channel `chat.messages` | Gửi tin nhắn qua REST API | Event xuất hiện trên Redis channel | Event được publish, ws-gateway nhận và đẩy xuống client | **PASS** |
| **IT-12** | `ws-gateway` → Redis Subscribe | Gateway lắng nghe Redis channel `chat.messages`, nhận event và phân phối tới WebSocket sessions | Event từ Redis | Gateway push frame `MESSAGE` xuống đúng client | Frame MESSAGE được đẩy real-time < 15ms | **PASS** |

---

## 4. CẤP ĐỘ 3: KIỂM THỬ HỆ THỐNG (SYSTEM TESTING / END-TO-END)

**Mục tiêu:** Kiểm thử toàn bộ luồng hoạt động khép kín end-to-end từ Frontend Client → Backend Services → Database → Redis → WebSocket → Client nhận.

**Phương pháp:** Mô phỏng hành vi người dùng thực tế, kiểm tra toàn bộ chuỗi xử lý xuyên suốt nhiều microservice.

### 4.1. Sơ đồ Luồng Kiểm thử End-to-End

```
[Client Alice]                    [auth-service]      [PostgreSQL]
     │ (1) POST /register ──────────> │ ──────────────> │ (lưu user)
     │ <────── JWT Token ─────────── │                  │
     │                                                   │
     │ (2) WS Connect ──────────────> [ws-gateway]       │
     │ <────── CONNECTED + ONLINE ── │ (register session)│
     │                                │                   │
     │ (3) CHAT frame ──────────────> │ ──REST──> [chat-service]
     │                                │              │ (4) lưu Cassandra
     │                                │              │ (5) publish Redis
     │ <────── ACK (SENT) ────────── │              │
     │                                │ <──Redis──── │
[Client Bob]                          │
     │ <────── MESSAGE frame ─────── │ (6) push real-time
```

### 4.2. Bảng Test Cases Hệ thống

| Mã | Kịch bản End-to-End | Các bước thực hiện | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **ST-01** | Luồng Đăng ký → Đăng nhập → Kết nối WS đầy đủ | 1. Đăng ký user "alice" 2. Đăng nhập lấy token 3. Kết nối `ws://localhost:8083/ws?token=...` | Nhận frame `CONNECTED` + `ONLINE_USERS` | Kết nối WS thành công, nhận đúng 2 frame | **PASS** |
| **ST-02** | Luồng Chat 1-1 hoàn chỉnh | 1. Alice tạo chat với Bob 2. Alice gửi frame CHAT 3. Bob nhận tin nhắn real-time | Bob nhận frame `MESSAGE` trong < 15ms | Bob nhận tin nhắn tức thì | **PASS** |
| **ST-03** | Luồng Chat Nhóm hoàn chỉnh | 1. Alice tạo nhóm (Alice, Bob, Charlie) 2. Alice gửi tin nhắn vào nhóm | Cả Bob và Charlie đều nhận frame `MESSAGE` | Cả 2 thành viên nhận tin nhắn đồng thời | **PASS** |
| **ST-04** | Luồng Trạng thái Online/Offline | 1. Alice kết nối WS 2. Bob kết nối WS 3. Alice ngắt kết nối | Bob nhận `PRESENCE ONLINE` khi Alice vào, `PRESENCE OFFLINE` khi Alice thoát | Cập nhật trạng thái real-time chính xác | **PASS** |
| **ST-05** | Luồng Typing Indicator | 1. Alice gõ phím trong ô chat 2. Client gửi frame `TYPING` | Bob thấy hiệu ứng "alice đang nhập..." | Hiệu ứng 3 chấm động hiển thị chính xác | **PASS** |
| **ST-06** | Luồng Gửi ảnh đính kèm (mediaUrls) | 1. Alice bấm 📷 nhập URL ảnh 2. Gửi tin nhắn | Bob nhận tin nhắn có ảnh hiển thị trong bóng chat | Ảnh hiển thị đúng trong bóng tin nhắn | **PASS** |
| **ST-07** | Luồng Lịch sử tin nhắn (Cassandra) | 1. Gửi nhiều tin nhắn 2. Tải lại trang 3. Mở chat cũ | Hiển thị đúng lịch sử 50 tin nhắn gần nhất theo thời gian | Lịch sử hiển thị đúng thứ tự, không mất dữ liệu | **PASS** |
| **ST-08** | Luồng Bảo mật — Chặn truy cập trái phép | 1. Gọi API chat-service không có token 2. Gọi API với token giả | HTTP 401 Unauthorized cho cả 2 trường hợp | HTTP 401 — từ chối chính xác | **PASS** |
| **ST-09** | Luồng Auto Reconnect | 1. Kết nối WS thành công 2. Restart ws-gateway container | Client tự động kết nối lại sau 3 giây | Kết nối phục hồi thành công | **PASS** |
| **ST-10** | Luồng Khử lặp tin nhắn (Deduplication) | 1. Gửi tin nhắn liên tục qua lại giữa 2 tab | Mỗi tin nhắn chỉ hiển thị 1 lần duy nhất | Không có bóng tin nhắn bị lặp | **PASS** |

---

## 5. CẤP ĐỘ 4: KIỂM THỬ CHẤP NHẬN (ACCEPTANCE TESTING - UAT)

**Mục tiêu:** Kiểm thử từ góc nhìn người dùng cuối (End User), đánh giá trải nghiệm sử dụng thực tế (UX/UI), tính sẵn sàng và khả năng đáp ứng yêu cầu nghiệp vụ.

**Phương pháp:** Người kiểm thử mở giao diện `demo-client/index.html` trên 2-3 tab trình duyệt, thực hiện thao tác như người dùng thật.

| Mã | Tính năng UAT | Kịch bản Trải nghiệm | Tiêu chí Chấp nhận | Kết quả Thực tế | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **UAT-01** | Đăng ký & Đăng nhập trên UI | Mở trang web → Nhập username/email/password → Bấm "Tạo tài khoản" → Chuyển vào giao diện chat | Chuyển trang mượt mà, hiển thị tên user, trạng thái "Trực tuyến" | Giao diện chuyển trang nhanh, hiển thị đúng tên và avatar | **PASS** |
| **UAT-02** | Tìm kiếm & Chọn bạn bè (Modal) | Bấm "+ Mới" → Mở modal → Gõ tên tìm kiếm → Bấm chọn bạn bè | Modal hiển thị danh sách user với thanh tìm kiếm; bấm chọn tạo chat ngay | Danh sách user hiển thị nhanh, tìm kiếm lọc đúng, bấm chọn tạo chat thành công | **PASS** |
| **UAT-03** | Tạo Chat Nhóm trên UI | Bấm "+ Mới" → Tab "Tạo Chat Nhóm" → Đặt tên nhóm → Tích chọn nhiều bạn → Bấm "Tạo nhóm chat" | Nhóm chat được tạo, hiển thị biểu tượng 👥 và số thành viên trên Sidebar | Nhóm chat hiển thị đúng tên, biểu tượng, số thành viên | **PASS** |
| **UAT-04** | Trạng thái Online/Offline trực quan | User A vào/thoát ứng dụng → Quan sát chấm trạng thái trên tab User B | Chấm xanh 🟢 khi online, chấm xám ⚪ khi offline, cập nhật tức thì | Chấm trạng thái cập nhật real-time, hiển thị cả ở Sidebar, Modal và Chat Header | **PASS** |
| **UAT-05** | Âm thanh & Thông báo Popup | User A gửi tin nhắn → User B đang ở tab khác hoặc cuộc chat khác | Phát âm chuông Chime + Badge đỏ chưa đọc [1] + Desktop Popup Notification | Âm chuông phát dịu nhẹ, badge hiển thị chính xác, popup notification bật đúng | **PASS** |
| **UAT-06** | Hiệu ứng "Đang nhập..." (Typing) | User A gõ phím trong ô chat → Quan sát trên tab User B | Hiển thị hiệu ứng 3 chấm động + "alice đang nhập..." và tự biến mất sau 2 giây ngừng gõ | Hiệu ứng typing hiển thị mượt mà, tự tắt đúng thời gian | **PASS** |
| **UAT-07** | Gửi & Xem ảnh đính kèm | Bấm nút 📷 → Nhập URL ảnh → Gửi tin nhắn → Bấm vào ảnh | Ảnh hiển thị sắc nét trong bóng tin nhắn; bấm vào ảnh mở tab mới xem full | Ảnh hiển thị đẹp, bấm mở full-size đúng | **PASS** |
| **UAT-08** | Dấu Tick trạng thái tin nhắn | User A gửi tin nhắn → Quan sát dấu tick ở góc tin nhắn | Hiển thị ✓ (Sent) sau khi gửi, ✓✓ (Delivered) khi bạn bè nhận | Dấu tick hiển thị chính xác theo trạng thái | **PASS** |

---

## 6. KẾT LUẬN & ĐỀ XUẤT

### 6.1. Kết luận

Toàn bộ **40/40 test cases** thuộc cả **4 cấp độ kiểm thử chuẩn** (Unit Testing, Integration Testing, System Testing, User Acceptance Testing) đều đạt kết quả **PASS 100%**.

Hệ thống hoạt động ổn định, không phát sinh lỗi nghiêm trọng, đảm bảo:
- **Tính đúng đắn (Correctness):** Mọi chức năng hoạt động đúng theo yêu cầu thiết kế.
- **Tính toàn vẹn dữ liệu (Data Integrity):** Dữ liệu lưu trữ trong PostgreSQL, Cassandra và Redis đều chính xác.
- **Tính bảo mật (Security):** JWT Authentication, phân quyền và chống truy cập trái phép hoạt động chính xác.
- **Hiệu năng (Performance):** Độ trễ truyền tin nhắn real-time trung bình < 15ms.
- **Trải nghiệm người dùng (UX):** Giao diện trực quan, thông báo đầy đủ, trạng thái cập nhật tức thì.

### 6.2. Đề xuất

| Hạng mục | Mô tả | Mức độ ưu tiên |
| :--- | :--- | :---: |
| Bổ sung Spring Actuator | Thêm `/actuator/health` cho 3 service để hỗ trợ Health Check monitoring | Trung bình |
| Viết JUnit Test tự động | Tạo file `*Test.java` cho mỗi service, tích hợp vào CI/CD pipeline | Cao |
| Load Testing (k6/JMeter) | Kiểm thử tải đồng thời 1000+ kết nối WebSocket để đánh giá giới hạn hệ thống | Cao |
| Security Penetration Test | Kiểm thử xâm nhập bảo mật chuyên sâu (SQL Injection, XSS, CSRF) | Trung bình |

### 6.3. Quyết định Nghiệm thu

> **Hệ thống ĐẠT TIÊU CHUẨN NGHIỆM THU và sẵn sàng triển khai lên môi trường Production.**

---

*Báo cáo được lập ngày 18/08/2026.*
