package com.chat.messaging.controller;

import com.chat.messaging.meme.Meme;
import com.chat.messaging.meme.MemeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/memes")
public class MemeController {

    private final MemeRepository memeRepository;

    public MemeController(MemeRepository memeRepository) {
        this.memeRepository = memeRepository;
    }

    @PostConstruct
    public void seedDatabase() {
        if (memeRepository.count() == 0) {
            // Seed initial popular memes
            memeRepository.save(new Meme(null, "https://i.imgflip.com/4t0m5.jpg", "HAPPY"));      // Doge Happy
            memeRepository.save(new Meme(null, "https://i.imgflip.com/1bip.jpg", "HAPPY"));       // Success Kid
            memeRepository.save(new Meme(null, "https://i.imgflip.com/1j24.jpg", "SAD"));         // Sad Pepe
            memeRepository.save(new Meme(null, "https://i.imgflip.com/43a45.jpg", "SAD"));        // Sad Wolverine
            memeRepository.save(new Meme(null, "https://i.imgflip.com/1o0la.jpg", "ANGRY"));      // Angry Baby
            memeRepository.save(new Meme(null, "https://i.imgflip.com/39t1m.jpg", "ANGRY"));      // Table Flip
            memeRepository.save(new Meme(null, "https://i.imgflip.com/2kfl4.jpg", "SURPRISED"));  // Surprised Pikachu
            memeRepository.save(new Meme(null, "https://i.imgflip.com/1w7y.jpg", "FEARFUL"));     // Screaming Meme
            memeRepository.save(new Meme(null, "https://i.imgflip.com/51y1.jpg", "NEUTRAL"));     // Poker Face
            memeRepository.save(new Meme(null, "https://i.imgflip.com/1ur9b0.jpg", "NEUTRAL"));   // Distracted Boyfriend
        }
    }

    @GetMapping("/search")
    public List<Meme> search(@RequestParam String tag) {
        String normalizedTag = tag.toUpperCase().trim();
        List<Meme> result = memeRepository.findByEmotion(normalizedTag);
        if (result.isEmpty()) {
            result = memeRepository.findByEmotion("NEUTRAL");
        }
        return result;
    }

    @GetMapping
    public List<Meme> getAllMemes() {
        return memeRepository.findAll();
    }

    @PostMapping
    public Meme addMeme(@RequestBody MemeRequest req) {
        Meme meme = new Meme(null, req.url(), req.emotion().toUpperCase().trim());
        return memeRepository.save(meme);
    }

    @DeleteMapping("/{id}")
    public void deleteMeme(@PathVariable UUID id) {
        memeRepository.deleteById(id);
    }

    public record MemeRequest(String url, String emotion) {}
}
