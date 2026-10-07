package com.chat.auth.controller;

import com.chat.auth.dto.AuthDtos.UserProfile;
import com.chat.auth.security.JwtService;
import com.chat.auth.user.User;
import com.chat.auth.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/auth/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final HttpClient httpClient;

    public AdminController(UserRepository userRepository, JwtService jwtService, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    private void checkAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không có token xác thực");
        }
        String token = authHeader.substring(7);
        try {
            Jws<Claims> claims = jwtService.parse(token);
            String role = claims.getPayload().get("role", String.class);
            if (!"ADMIN".equals(role)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền Admin");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn");
        }
    }

    @GetMapping("/users")
    public List<UserProfile> getAllUsers(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        checkAdmin(authHeader);
        return userRepository.findAll().stream()
                .map(user -> new UserProfile(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isBanned(), user.getCreatedAt()))
                .toList();
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> updateUserRole(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        checkAdmin(authHeader);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));
        String newRole = body.get("role");
        if (newRole == null || (!"USER".equals(newRole) && !"ADMIN".equals(newRole))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role không hợp lệ");
        }
        user.setRole(newRole);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{id}/ban")
    public ResponseEntity<Void> banUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body) {
        checkAdmin(authHeader);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user"));
        boolean ban = body.getOrDefault("ban", false);
        user.setBanned(ban);
        userRepository.save(user);

        // Phát tín hiệu đóng kết nối WebSocket ngay lập tức thông qua Redis Pub/Sub
        redisTemplate.convertAndSend("admin.actions", "BAN:" + id.toString());

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {
        checkAdmin(authHeader);
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user");
        }
        userRepository.deleteById(id);
        
        // Phát tín hiệu đóng kết nối WebSocket ngay lập tức nếu đang online
        redisTemplate.convertAndSend("admin.actions", "BAN:" + id.toString());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/system/stats")
    public Map<String, Object> getSystemStats(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        checkAdmin(authHeader);
        
        long totalUsers = userRepository.count();
        
        // Lấy số lượng online users từ Redis Set online:users
        Long onlineUsers = redisTemplate.opsForSet().size("online:users");
        if (onlineUsers == null) onlineUsers = 0L;
        
        // Gọi chat-service lấy số lượng cuộc trò chuyện
        long totalConversations = 0;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://chat-service:8082/api/admin/chat/stats"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                // Parse conversations count (định dạng json: {"count": 123})
                String countStr = resp.body();
                if (countStr.contains("\"count\":")) {
                    String sub = countStr.substring(countStr.indexOf("\"count\":") + 8);
                    sub = sub.replaceAll("[^0-9]", "");
                    totalConversations = Long.parseLong(sub);
                }
            }
        } catch (Exception e) {
            // Log error, chat-service có thể offline hoặc chưa tạo admin API
            totalConversations = -1;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("onlineUsers", onlineUsers);
        stats.put("totalConversations", totalConversations);
        return stats;
    }

    @GetMapping("/system/health")
    public Map<String, Object> getSystemHealth(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        checkAdmin(authHeader);

        Map<String, Object> health = new HashMap<>();
        health.put("auth-service", checkUrlHealth("http://localhost:8081/actuator/health"));
        health.put("chat-service", checkUrlHealth("http://chat-service:8082/actuator/health"));
        health.put("ws-gateway", checkUrlHealth("http://ws-gateway:8083/actuator/health"));
        return health;
    }

    private String checkUrlHealth(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body().contains("\"status\":\"UP\"")) {
                return "UP";
            }
            return "DOWN (" + resp.statusCode() + ")";
        } catch (Exception e) {
            return "DOWN (Exception)";
        }
    }

    @GetMapping("/system/maintenance")
    public Map<String, Boolean> getMaintenanceStatus() {
        String status = redisTemplate.opsForValue().get("system:maintenance");
        Map<String, Boolean> res = new HashMap<>();
        res.put("maintenance", "true".equals(status));
        return res;
    }

    @PostMapping("/system/maintenance")
    public ResponseEntity<Void> toggleMaintenance(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Boolean> body) {
        checkAdmin(authHeader);
        boolean maintenance = body.getOrDefault("maintenance", false);
        redisTemplate.opsForValue().set("system:maintenance", maintenance ? "true" : "false");
        
        // Ghi log bảo mật
        String logEntry = String.format("{\"timestamp\":%d,\"ip\":\"internal\",\"path\":\"/system/maintenance\",\"reason\":\"Thay đổi chế độ bảo trì thành %b\",\"username\":\"admin\"}",
                System.currentTimeMillis(), maintenance);
        redisTemplate.opsForList().leftPush("security:logs", logEntry);
        redisTemplate.opsForList().trim("security:logs", 0, 99);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/system/blacklist-ip")
    public Set<String> getBlacklistIps(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        checkAdmin(authHeader);
        Set<String> ips = redisTemplate.opsForSet().members("blacklist:ips");
        return ips != null ? ips : Collections.emptySet();
    }

    @PostMapping("/system/blacklist-ip")
    public ResponseEntity<Void> addBlacklistIp(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {
        checkAdmin(authHeader);
        String ip = body.get("ip");
        if (ip != null && !ip.isBlank()) {
            redisTemplate.opsForSet().add("blacklist:ips", ip.trim());
            
            // Ghi log bảo mật
            String logEntry = String.format("{\"timestamp\":%d,\"ip\":\"%s\",\"path\":\"/system/blacklist-ip\",\"reason\":\"Thêm IP vào blacklist\",\"username\":\"admin\"}",
                    System.currentTimeMillis(), ip);
            redisTemplate.opsForList().leftPush("security:logs", logEntry);
            redisTemplate.opsForList().trim("security:logs", 0, 99);
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/system/blacklist-ip/{ip}")
    public ResponseEntity<Void> removeBlacklistIp(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String ip) {
        checkAdmin(authHeader);
        redisTemplate.opsForSet().remove("blacklist:ips", ip.trim());
        
        // Ghi log bảo mật
        String logEntry = String.format("{\"timestamp\":%d,\"ip\":\"%s\",\"path\":\"/system/blacklist-ip\",\"reason\":\"Xóa IP khỏi blacklist\",\"username\":\"admin\"}",
                System.currentTimeMillis(), ip);
        redisTemplate.opsForList().leftPush("security:logs", logEntry);
        redisTemplate.opsForList().trim("security:logs", 0, 99);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/system/logs")
    public List<String> getSecurityLogs(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        checkAdmin(authHeader);
        List<String> logs = redisTemplate.opsForList().range("security:logs", 0, -1);
        return logs != null ? logs : Collections.emptyList();
    }

    @GetMapping("/stats/tokens")
    public Map<Object, Object> getTokenStats(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        checkAdmin(authHeader);
        Map<Object, Object> stats = redisTemplate.opsForHash().entries("stats:tokens:generated");
        return stats != null ? stats : Collections.emptyMap();
    }

    @GetMapping("/stats/requests")
    public Map<Object, Object> getRequestStats(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        checkAdmin(authHeader);
        Map<Object, Object> stats = redisTemplate.opsForHash().entries("stats:api:requests");
        return stats != null ? stats : Collections.emptyMap();
    }

    // ── SYSTEM BROADCAST ──

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcastMessage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {
        checkAdmin(authHeader);
        String message = body.getOrDefault("message", "");
        String broadcastType = body.getOrDefault("type", "INFO");
        if (message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung thông báo không được để trống");
        }

        // Publish qua Redis Pub/Sub, ws-gateway sẽ nhận và broadcast tới tất cả clients
        String payload = String.format("BROADCAST:%s|%s", broadcastType, message);
        redisTemplate.convertAndSend("admin.actions", payload);

        // Ghi security log
        String logEntry = String.format("{\"timestamp\":%d,\"ip\":\"internal\",\"path\":\"/admin/broadcast\",\"reason\":\"Phát thông báo: %s\",\"username\":\"admin\"}",
                System.currentTimeMillis(), message.replace("\"", "'").substring(0, Math.min(message.length(), 100)));
        redisTemplate.opsForList().leftPush("security:logs", logEntry);
        redisTemplate.opsForList().trim("security:logs", 0, 99);

        return ResponseEntity.ok().build();
    }

    // ── MUTE USER ──

    @PostMapping("/users/{id}/mute")
    public ResponseEntity<Void> muteUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Integer> body) {
        checkAdmin(authHeader);
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user");
        }
        int durationMinutes = body.getOrDefault("durationMinutes", 30);
        redisTemplate.opsForValue().set("mute:user:" + id.toString(), "true",
                Duration.ofMinutes(durationMinutes));

        // Ghi log
        String logEntry = String.format("{\"timestamp\":%d,\"ip\":\"internal\",\"path\":\"/admin/users/%s/mute\",\"reason\":\"Cấm chat %d phút\",\"username\":\"admin\"}",
                System.currentTimeMillis(), id, durationMinutes);
        redisTemplate.opsForList().leftPush("security:logs", logEntry);
        redisTemplate.opsForList().trim("security:logs", 0, 99);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}/mute")
    public ResponseEntity<Void> unmuteUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {
        checkAdmin(authHeader);
        redisTemplate.delete("mute:user:" + id.toString());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{id}/mute")
    public Map<String, Object> getMuteStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable UUID id) {
        checkAdmin(authHeader);
        Boolean muted = redisTemplate.hasKey("mute:user:" + id.toString());
        Long ttl = redisTemplate.getExpire("mute:user:" + id.toString());
        Map<String, Object> result = new HashMap<>();
        result.put("muted", Boolean.TRUE.equals(muted));
        result.put("remainingSeconds", ttl != null && ttl > 0 ? ttl : 0);
        return result;
    }

    // ── USER GROWTH STATS ──

    @GetMapping("/stats/user-growth")
    public List<Map<String, Object>> getUserGrowthStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "30") int days) {
        checkAdmin(authHeader);
        return userRepository.findUserGrowthStats(days);
    }
}
