package com.chat.messaging.meme;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemeRepository extends JpaRepository<Meme, UUID> {
    List<Meme> findByEmotion(String emotion);
}
