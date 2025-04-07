package cn.wth.ai.repository;

import java.util.List;

/**
 * @Author: 5th
 * @Description: 会话记录接口
 * @CreateTime: 2025-04-07 23:03
 */
public interface ChatHistoryRepository {

    /**
     * 保存会话记录
     * @param type 业务类型，如：chat、service、pdf
     * @param chatId 会话ID
     */
    void save(String type, String chatId);

    /**
     * 获取会话ID列表
     * @param type 业务类型，如：chat、service、pdf
     * @return 会话ID列表
     */
    List<String> getChatIds(String type);

}
