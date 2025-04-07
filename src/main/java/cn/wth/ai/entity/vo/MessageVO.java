package cn.wth.ai.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.Message;

/**
 * @Author: 5th
 * @Description: 接受Message信息
 * @CreateTime: 2025-04-08 00:45
 */
@Data
@NoArgsConstructor
public class MessageVO {
    private String role;
    private String content;

    public MessageVO(Message message) {
        this.role = switch (message.getMessageType()) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            default -> "";
        };
        this.content = message.getText();
    }
}
