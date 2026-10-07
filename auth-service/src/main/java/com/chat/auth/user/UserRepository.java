package com.chat.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Thống kê số user đăng ký mới theo ngày */
    @Query(value = """
            SELECT CAST(created_at AS DATE) as date, COUNT(*) as count
            FROM users
            GROUP BY CAST(created_at AS DATE)
            ORDER BY date DESC
            LIMIT :days
            """, nativeQuery = true)
    List<Map<String, Object>> findUserGrowthStats(@Param("days") int days);
}
