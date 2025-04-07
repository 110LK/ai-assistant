package cn.wth.ai.repository.impl;

import cn.wth.ai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Author: 5th
 * @Description: 会话记录接口实现
 * @CreateTime: 2025-04-07 23:06
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {
    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String type, String chatId) {
        try {
            String key = buildKey(type);
            Boolean isMember = redisTemplate.opsForSet().isMember(key, chatId);

            if (Boolean.TRUE.equals(isMember)) {
                log.debug("ChatId {} already exists for type {}", chatId, type);
                return;
            }

            redisTemplate.opsForSet().add(key, chatId);
            log.debug("Successfully saved chatId {} for type {}", chatId, type);
        } catch (Exception e) {
            log.error("Error saving chatId {} for type {} to Redis", chatId, type, e);
            throw new RuntimeException("Failed to save chat history to Redis", e);
        }
    }

    @Override
    public List<String> getChatIds(String type) {
        try {
            String key = buildKey(type);
            Set<String> chatIds = redisTemplate.opsForSet().members(key);

            if (chatIds == null || chatIds.isEmpty()) {
                log.debug("No chat IDs found for type {}", type);
                return List.of();
            }

            log.debug("Retrieved {} chat IDs for type {}", chatIds.size(), type);
            return new ArrayList<>(chatIds);
        } catch (Exception e) {
            log.error("Error retrieving chat IDs for type {} from Redis", type, e);
            throw new RuntimeException("Failed to retrieve chat history from Redis", e);
        }
    }

    private String buildKey(String type) {
        return "chat:history:" + type;
    }
}
