package com.chat.messaging.controller;

import com.chat.messaging.conversation.ConversationMemberRepository;
import com.chat.messaging.conversation.ConversationRepository;
import com.chat.messaging.meme.MemeRepository;
import com.chat.messaging.message.MessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final MemeRepository memeRepository;
    private final StringRedisTemplate redisTemplate;

    public AdminChatController(ConversationRepository conversationRepository,
                               ConversationMemberRepository memberRepository,
                               MessageRepository messageRepository,
                               MemeRepository memeRepository,
                               StringRedisTemplate redisTemplate) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.memeRepository = memeRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Kiểm tra quyền ADMIN từ JWT claim "role".
     * Ném 403 Forbidden nếu không phải ADMIN.
     */
    private void requireAdmin(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không có token xác thực");
        }
        String role = jwt.getClaimAsString("role");
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền Admin");
        }
    }

    /** Thống kê tổng số cuộc trò chuyện — cho phép auth-service gọi nội bộ (đã authenticated) */
    @GetMapping("/stats")
    public Map<String, Object> getChatStats() {
        long count = conversationRepository.count();
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        return response;
    }

    /** Message Throughput: số tin nhắn mỗi phút trong 30 phút qua */
    @GetMapping("/stats/throughput")
    public Map<String, Object> getMessageThroughput(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        long currentMinute = System.currentTimeMillis() / 60000;
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        for (int i = 29; i >= 0; i--) {
            String minuteKey = String.valueOf(currentMinute - i);
            Object val = redisTemplate.opsForHash().get("stats:msg:throughput", minuteKey);
            long count = val != null ? Long.parseLong(val.toString()) : 0;
            dataPoints.add(Map.of(
                    "minute", currentMinute - i,
                    "count", count
            ));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dataPoints", dataPoints);
        return result;
    }

    /** Top Users: top N người dùng gửi nhiều tin nhắn nhất */
    @GetMapping("/stats/top-users")
    public List<Map<String, Object>> getTopUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "10") int limit) {
        requireAdmin(jwt);
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores("stats:user:msg_count", 0, limit - 1);
        if (tuples == null) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("rank", rank++);
            entry.put("userId", tuple.getValue());
            entry.put("messageCount", tuple.getScore() != null ? tuple.getScore().longValue() : 0);
            result.add(entry);
        }
        return result;
    }

    /** Peak Hours: phân bố tin nhắn theo giờ trong ngày (0-23) */
    @GetMapping("/stats/peak-hours")
    public Map<String, Long> getPeakHours(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        Map<String, Long> hours = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) {
            String key = String.valueOf(h);
            Object val = redisTemplate.opsForHash().get("stats:msg:hourly", key);
            hours.put(key, val != null ? Long.parseLong(val.toString()) : 0);
        }
        return hours;
    }

    /** Danh sách tất cả conversations (cho admin quản lý) */
    @GetMapping("/conversations")
    public Map<String, Object> listConversations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(jwt);
        var pageResult = conversationRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Map<String, Object>> items = new ArrayList<>();
        for (var conv : pageResult.getContent()) {
            long memberCount = memberRepository.findByIdConversationId(conv.getId()).size();
            Map<String, Object> item = new HashMap<>();
            item.put("id", conv.getId());
            item.put("type", conv.getType());
            item.put("name", conv.getName());
            item.put("memberCount", memberCount);
            item.put("createdAt", conv.getCreatedAt());
            items.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", items);
        result.put("totalElements", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("currentPage", page);
        return result;
    }

    /** Xóa một conversation (admin) */
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        requireAdmin(jwt);
        if (!conversationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // Xóa members trước
        var members = memberRepository.findByIdConversationId(id);
        memberRepository.deleteAll(members);
        // Xóa conversation
        conversationRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /** Thống kê lưu trữ */
    @GetMapping("/stats/storage")
    public Map<String, Object> getStorageStats(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        long totalMemes = memeRepository.count();
        // Cassandra message count (ước lượng)
        long totalMessages = 0;
        try {
            totalMessages = messageRepository.count();
        } catch (Exception ignored) {
            // count() trên Cassandra có thể chậm hoặc không hỗ trợ tốt
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalMessages", totalMessages);
        result.put("totalMemes", totalMemes);
        result.put("totalConversations", conversationRepository.count());
        return result;
    }
}
