package cn.wth.ai.config;

import cn.wth.ai.chat.RedisChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 5th
 * @Description: AI对话模型配置类
 * @CreateTime: 2025-04-07 18:50
 */
@Configuration
public class CommonConfiguration {

    @Bean
    public ChatClient chatClient(OllamaChatModel ollamaChatModel, RedisChatMemory redisChatMemory) {
        return ChatClient
                .builder(ollamaChatModel)   // 创建ChatClient工厂实例
                .defaultSystem("你是一个热心、可爱的智能助手，你的名字叫小五，请以小五的身份和语气回答问题。")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),  // 历史会话保存到Redis中
                        new MessageChatMemoryAdvisor(redisChatMemory)) // 添加默认的Advisor，记录日志
                .build();
    }

}
