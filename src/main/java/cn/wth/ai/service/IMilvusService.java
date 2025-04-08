package cn.wth.ai.service;

import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;

/**
 * @Author: 5th
 * @Description: Milvus服务接口
 * @CreateTime: 2025-04-09 00:02
 */
public interface IMilvusService {

    /**
     * 检查集合是否存在,不存在则创建集合
     */
    void hasCollection();

    /**
     * 插入数据
     *
     * @param vectorParam   向量参数
     * @param text          文本
     * @param metadata      元数据
     * @param fileName      文件名
     * @return
     */
    InsertResp insert(float[] vectorParam, String text, String metadata, String fileName);

    /**
     * 搜索数据
     *
     * @param vectorParam   向量参数
     * @return
     */
    SearchResp search(float[] vectorParam);

}
