package cn.wth.ai.service.impl;

import cn.wth.ai.constants.MilvusArchive;
import cn.wth.ai.service.IMilvusService;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;

import io.milvus.v2.service.collection.request.*;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: 5th
 * @Description: Milvus服务实现类
 * @CreateTime: 2025-04-09 00:03
 */
@Service
@RequiredArgsConstructor
public class MilvusServiceImpl implements IMilvusService {

    private MilvusClientV2 milvusClientV2;

    private final OpenAiEmbeddingModel embeddingModel;

    @Override
    public void hasCollection() {
        Boolean flag = milvusClientV2.hasCollection(HasCollectionReq.builder().collectionName(MilvusArchive.COLLECTION_NAME).build());
        if (!flag) {
            this.createCollection();
        }
    }

    @Override
    public InsertResp insert(float[] vectorParam, String text, String metadata, String fileName) {
        // 校验集合是否存在
        this.hasCollection();
        JsonObject jsonObject = new JsonObject();
        // 数组转换成JsonElement
        jsonObject.add(MilvusArchive.Field.FEATURE, new Gson().toJsonTree(vectorParam));
        jsonObject.add(MilvusArchive.Field.TEXT, new Gson().toJsonTree(text));
        jsonObject.add(MilvusArchive.Field.METADATA, new Gson().toJsonTree(metadata));
        jsonObject.add(MilvusArchive.Field.FILE_NAME, new Gson().toJsonTree(fileName));
        InsertReq insertReq = InsertReq.builder()
                // 集合名称
                .collectionName(MilvusArchive.COLLECTION_NAME)
                .data(Collections.singletonList(jsonObject))
                .build();

        return milvusClientV2.insert(insertReq);

    }

    @Override
    public SearchResp search(float[] vectorParam) {
        this.loadCollection();
        FloatVec floatVec = new FloatVec(vectorParam);
        SearchReq searchReq = SearchReq.builder()
                // 集合名称
                .collectionName(MilvusArchive.COLLECTION_NAME)
                // 搜索距离度量
                .metricType(IndexParam.MetricType.COSINE)
                // 搜索向量
                .data(Collections.singletonList(floatVec))
                // 搜索字段
                .annsField(MilvusArchive.Field.FEATURE)
                // 返回字段
                .outputFields(Arrays.asList(MilvusArchive.Field.ID, MilvusArchive.Field.TEXT, MilvusArchive.Field.METADATA, MilvusArchive.Field.FILE_NAME))
                // 搜索数量
                .topK(5)
                .build();
        return milvusClientV2.search(searchReq);

    }

    @Override
    public InsertResp batchInsert(List<float[]> vectorParam, List<String> text, List<String> metadata, List<String> fileName) {
        if (vectorParam.size() == text.size() && vectorParam.size() == metadata.size() && vectorParam.size() == fileName.size()) {
            List<JsonObject> jsonObjects = new ArrayList<>();
            for (int i = 0; i < vectorParam.size(); i++) {
                JsonObject jsonObject = new JsonObject();
                // 数组转换成JsonElement
                jsonObject.add(MilvusArchive.Field.FEATURE, new Gson().toJsonTree(vectorParam.get(i)));
                jsonObject.add(MilvusArchive.Field.TEXT, new Gson().toJsonTree(text.get(i)));
                jsonObject.add(MilvusArchive.Field.METADATA, new Gson().toJsonTree(metadata.get(i)));
                jsonObject.add(MilvusArchive.Field.FILE_NAME, new Gson().toJsonTree(fileName.get(i)));
                jsonObjects.add(jsonObject);
            }
            InsertReq insertReq = InsertReq.builder()
                    // 集合名称
                    .collectionName(MilvusArchive.COLLECTION_NAME)
                    .data(jsonObjects)
                    .build();
            return milvusClientV2.insert(insertReq);
        }
        return null;
    }

    @Override
    public DeleteResp delete(String[] ids) {
        // 将String数组转换为Long类型（假设输入的id是合法的Long类型字符串）
        List<Long> idList = Arrays.stream(ids)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 构建删除表达式
        String expr = String.format("%s in %s", MilvusArchive.Field.ID, idList.toString());

        // 构造删除请求
        DeleteReq deleteReq = DeleteReq.builder()
                .collectionName(MilvusArchive.COLLECTION_NAME)
                .filter(expr)
                .build();

        return milvusClientV2.delete(deleteReq);
    }

    @Override
    public DeleteResp delete(String id) {
        // 将字符串ID转换为Long类型（根据主键字段类型）
        Long longId = Long.parseLong(id);

        // 构造删除表达式
        String expr = String.format("%s == %d", MilvusArchive.Field.ID, longId);

        // 构建删除请求
        DeleteReq deleteReq = DeleteReq.builder()
                .collectionName(MilvusArchive.COLLECTION_NAME)
                .filter(expr)
                .build();

        return milvusClientV2.delete(deleteReq);
    }

    public List<Document> search(SearchRequest request) {
        // 转换请求参数
        float[] vectorParam = embeddingModel.embed(request.getQuery());
        SearchResp search = search(vectorParam);
        return convertSearchResponse(search);
    }

    public List<Document> convertSearchResponse(SearchResp searchResp) {
        List<Document> documents = new ArrayList<>();

        // 遍历每个搜索结果组
        for (List<SearchResp.SearchResult> resultGroup : searchResp.getSearchResults()) {
            for (SearchResp.SearchResult result : resultGroup) {
                // 提取字段
                Map<String, Object> metadata = new HashMap<>();
                String content = "";

                // 解析实体字段（根据集合定义的字段）
                Map<String, Object> entity = result.getEntity();
                if (entity.containsKey(MilvusArchive.Field.TEXT)) {
                    content = entity.get(MilvusArchive.Field.TEXT).toString();
                }
                if (entity.containsKey(MilvusArchive.Field.ID)) {
                    metadata.put("id", entity.get(MilvusArchive.Field.ID));
                }
                if (entity.containsKey(MilvusArchive.Field.METADATA)) {
                    metadata.put("metadata", entity.get(MilvusArchive.Field.METADATA));
                }
                if (entity.containsKey(MilvusArchive.Field.FILE_NAME)) {
                    metadata.put("filename", entity.get(MilvusArchive.Field.FILE_NAME));
                }

                // 添加相似度得分
                metadata.put("similarityScore", result.getScore());

                // 创建Spring AI文档对象
                documents.add(new Document(content, metadata));
            }
        }
        return documents;
    }


    private void loadCollection() {
        LoadCollectionReq loadReq = LoadCollectionReq.builder()
                .collectionName(MilvusArchive.COLLECTION_NAME)
                .build();
        milvusClientV2.loadCollection(loadReq);
    }

//    private void loadCollection() {
//        // 1. 构建集合加载请求
//        LoadCollectionReq loadReq = LoadCollectionReq.builder()
//                .collectionName(MilvusArchive.COLLECTION_NAME)
//                .build();
//
//        // 2. 检查集合加载状态
//        DescribeCollectionReq describeReq = DescribeCollectionReq.builder()
//                .collectionName(MilvusArchive.COLLECTION_NAME)
//                .build();
//
//        // 3. 仅当集合未加载时执行加载
//        if (!milvusClientV2.describeCollection(describeReq).isLoaded()) {
//            milvusClientV2.loadCollection(loadReq);
//        }
//    }


    /**
     * 创建集合
     */
    public void createCollection() {
        // 创建字段
        CreateCollectionReq.CollectionSchema schema = milvusClientV2.createSchema()
                // 创建主键字段
                .addField(AddFieldReq.builder()
                        // 字段名
                        .fieldName(MilvusArchive.Field.ID)
                        // 字段描述
                        .description("主键ID")
                        // 字段类型
                        .dataType(DataType.Int64)
                        // 是否为主键
                        .isPrimaryKey(true)
                        // 设置主键自增
                        .autoID(true)
                        .build())
                .addField(AddFieldReq.builder()
                        // 字段名
                        .fieldName(MilvusArchive.Field.FILE_NAME)
                        // 字段描述
                        .description("文件名")
                        // 字段类型
                        .dataType(DataType.VarChar)
                        // 设置字段为可空
                        .isNullable(true)
                        .build())
                // 创建特征向量字段
                .addField(AddFieldReq.builder()
                        // 字段名
                        .fieldName(MilvusArchive.Field.FEATURE)
                        // 字段描述
                        .description("特征向量")
                        // 字段类型
                        .dataType(DataType.FloatVector)
                        // 设置向量维度
                        .dimension(MilvusArchive.FEATURE_DIM)
                        .build())
                .addField(AddFieldReq.builder()
                        // 字段名
                        .fieldName(MilvusArchive.Field.TEXT)
                        // 字段描述
                        .description("文本")
                        // 字段类型
                        .dataType(DataType.VarChar)
                        // 设置字段为可空
                        .isNullable(true)
                        .build())
                .addField(AddFieldReq.builder()
                        // 字段名
                        .fieldName(MilvusArchive.Field.METADATA)
                        // 字段描述
                        .description("元数据")
                        // 字段类型
                        .dataType(DataType.VarChar)
                        // 设置字段为可空
                        .isNullable(true)
                        .build());
        // 创建集合
        CreateCollectionReq collectionReq = CreateCollectionReq.builder()
                // 集合名称
                .collectionName(MilvusArchive.COLLECTION_NAME)
                // 集合描述
                .description("自定义知识库")
                // 集合字段
                .collectionSchema(schema)
                // 分片数量
                .numShards(MilvusArchive.SHARDS_NUM)
                .build();
        milvusClientV2.createCollection(collectionReq);

        // 创建索引
        IndexParam indexParam = IndexParam.builder()
                // 索引字段名
                .fieldName(MilvusArchive.Field.FEATURE)
                // 索引类型
                .indexType(IndexParam.IndexType.IVF_FLAT)
                // 索引距离度量
                .metricType(IndexParam.MetricType.COSINE)
                .build();
        CreateIndexReq createIndexReq = CreateIndexReq.builder()
                .collectionName(MilvusArchive.COLLECTION_NAME)
                .indexParams(Collections.singletonList(indexParam))
                .build();

        milvusClientV2.createIndex(createIndexReq);
    }


}