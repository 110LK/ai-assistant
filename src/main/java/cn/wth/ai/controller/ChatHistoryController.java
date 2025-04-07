package cn.wth.ai.controller;

import cn.wth.ai.chat.RedisChatMemory;
import cn.wth.ai.entity.vo.MessageVO;
import cn.wth.ai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: 5th
 * @Description: 查询会话历史
 * @CreateTime: 2025-04-08 00:39
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    private final ChatHistoryRepository chatHistoryRepository;

    private final RedisChatMemory redisChatMemory;

    /**
     * 查询会话历史列表
     * @param type 业务类型，如：chat,service,pdf
     * @return chatId列表
     */
    @GetMapping("/{type}")
    public List<String> getChatIds(@PathVariable("type") String type) {
        return chatHistoryRepository.getChatIds(type);
    }

    /**
     * 根据业务类型、chatId查询会话历史
     * @param type 业务类型，如：chat,service,pdf
     * @param chatId 会话id
     * @return 指定会话的历史消息
     */
    @GetMapping("/{type}/{chatId}")
    public List<MessageVO> getChatHistory(@PathVariable("type") String type, @PathVariable("chatId") String chatId) {
        List<Message> messages = redisChatMemory.get(chatId, Integer.MAX_VALUE);
        if(messages == null) {
            return List.of();
        }
        return messages.stream().map(MessageVO::new).toList();
    }
}
