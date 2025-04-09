package cn.wth.ai.customization;

import cn.wth.ai.service.IMilvusService;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @Author: 5th
 * @Description: 自定义向量数据库相关接口实现
 * @CreateTime: 2025-04-09 17:05
 */
@Component
@RequiredArgsConstructor
public class MilvusVectorStore implements VectorStore {

    private final IMilvusService milvusService;

    private final OpenAiEmbeddingModel embeddingModel;

    @Override
    public void add(List<Document> documents) {
        if (!documents.isEmpty()) {
            for (Document document : documents) {
                milvusService.insert(embeddingModel.embed(document), document.getText(), JSON.toJSONString(document.getMetadata()), null);
            }
        }
    }

    @Override
    public void delete(List<String> idList) {
        if (!idList.isEmpty()) {
            // idList转换为id数组
            String[] ids = idList.toArray(new String[0]);
            milvusService.delete(ids);
        }
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        milvusService.delete(filterExpression.toString());
    }

    @Override
    public List<Document> similaritySearch(@NotNull SearchRequest request) {
        return milvusService.search(request);
    }

}
