package com.chat.auth.service;

import com.chat.auth.dto.AuthDtos.AuthResponse;
import com.chat.auth.dto.AuthDtos.LoginRequest;
import com.chat.auth.dto.AuthDtos.RefreshRequest;
import com.chat.auth.dto.AuthDtos.RegisterRequest;
import com.chat.auth.dto.AuthDtos.UserProfile;
import com.chat.auth.exception.AuthExceptions.DuplicateUserException;
import com.chat.auth.exception.AuthExceptions.InvalidCredentialsException;
import com.chat.auth.exception.AuthExceptions.InvalidTokenException;
import com.chat.auth.exception.AuthExceptions.UserNotFoundException;
import com.chat.auth.security.JwtService;
import com.chat.auth.user.User;
import com.chat.auth.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUserException("Username đã tồn tại: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("Email đã tồn tại: " + request.email());
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Sai tài khoản hoặc mật khẩu"));
        if (user.isBanned()) {
            throw new InvalidCredentialsException("Tài khoản đã bị khóa bởi Admin.");
        }
        
        // Kiểm tra chế độ bảo trì
        if (redisTemplate != null) {
            try {
                String maintenance = redisTemplate.opsForValue().get("system:maintenance");
                if ("true".equals(maintenance) && !"ADMIN".equals(user.getRole())) {
                    throw new InvalidCredentialsException("Hệ thống đang bảo trì nâng cấp. Chỉ Admin mới có quyền đăng nhập.");
                }
            } catch (Exception ignored) {
            }
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Sai tài khoản hoặc mật khẩu");
        }
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        Jws<Claims> jws;
        try {
            jws = jwtService.parse(request.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        if (!"refresh".equals(jws.getPayload().get("typ", String.class))) {
            throw new InvalidTokenException("Token không phải loại refresh");
        }
        UUID userId = UUID.fromString(jws.getPayload().getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("Người dùng không tồn tại"));
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy user: " + userId));
        return new UserProfile(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isBanned(), user.getCreatedAt());
    }

    @Transactional
    public UserProfile updateProfile(UUID userId, com.chat.auth.dto.AuthDtos.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy user: " + userId));
        if (request.username() != null && !request.username().isBlank() && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new DuplicateUserException("Username đã được sử dụng: " + request.username());
            }
            user.setUsername(request.username());
        }
        if (request.email() != null && !request.email().isBlank() && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new DuplicateUserException("Email đã được sử dụng: " + request.email());
            }
            user.setEmail(request.email());
        }
        userRepository.save(user);
        return new UserProfile(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isBanned(), user.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<UserProfile> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserProfile(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isBanned(), user.getCreatedAt()))
                .toList();
    }

    private AuthResponse issueTokens(User user) {
        // Ghi nhận số lượng token được tạo cho từng tài khoản vào Redis Hash
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForHash().increment("stats:tokens:generated", user.getUsername(), 1);
            } catch (Exception ignored) {
            }
        }

        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                user.getId(),
                user.getUsername(),
                user.getRole(),
                jwtService.getAccessTokenTtlSeconds());
    }
}
