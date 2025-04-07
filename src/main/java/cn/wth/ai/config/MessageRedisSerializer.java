package cn.wth.ai.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.IOException;

/**
 * @Author: 5th
 * @Description: Message消息序列化器
 * @CreateTime: 2025-04-07 22:26
 */
public class MessageRedisSerializer implements RedisSerializer<Message> {

    // 负责JSON序列化/反序列化
    private final ObjectMapper objectMapper;
    // 自定义的反序列化器
    private final JsonDeserializer<Message> messageDeserializer;

    public MessageRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.messageDeserializer = new JsonDeserializer<>() {
            @Override
            public Message deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
                // 解析JSON为树形结构
                ObjectNode root = jp.readValueAsTree();
                // 提取类型标识
                String type = root.get("messageType").asText();
                // 根据类型创建子类对象
                return switch (type) {
                    case "USER" -> new UserMessage(root.get("text").asText());
                    case "ASSISTANT" -> new AssistantMessage(root.get("text").asText());
                    default -> throw new UnsupportedOperationException("未知的消息类型");
                };
            }
        };
    }

    // 将Message对象序列化为JSON字节数组
    @Override
    public byte[] serialize(Message message) {
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("无法序列化", e);
        }
    }

    @Override
    public Message deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return messageDeserializer.deserialize(objectMapper.getFactory().createParser(bytes), objectMapper.getDeserializationContext());
        } catch (Exception e) {
            throw new RuntimeException("无法反序列化", e);
        }
    }
}