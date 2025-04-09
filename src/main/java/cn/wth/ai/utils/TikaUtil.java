package cn.wth.ai.utils;

import cn.wth.ai.entity.vo.TikaVO;
import com.alibaba.fastjson2.JSON;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: 5th
 * @Description: Tika文件内容提取工具
 * @CreateTime: 2025-04-09 11:04
 */
@Component
@RequiredArgsConstructor
public class TikaUtil {

    // 目标段落长度（汉字字符数）
    private static final int TARGET_LENGTH = 200;
    // 允许的段落长度浮动范围（±20字）
    private static final int LENGTH_TOLERANCE = 20;

    public String extractTextString(MultipartFile file) {
        try {
            // 创建解析器--在不确定文档类型时候可以选择使用AutoDetectParser可以自动检测一个最合适的解析器
            Parser parser = new AutoDetectParser();
            // 用于捕获文档提取的文本内容。-1 参数表示使用无限缓冲区,解析到的内容通过此hander获取
            BodyContentHandler bodyContentHandler = new BodyContentHandler(-1);
            // 元数据对象，它在解析器中传递元数据属性---可以获取文档属性
            Metadata metadata = new Metadata();
            // 带有上下文相关信息的ParseContext实例，用于自定义解析过程。
            ParseContext parseContext = new ParseContext();
            parser.parse(file.getInputStream(), bodyContentHandler, metadata, parseContext);
            // 获取文本
            return bodyContentHandler.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 使用HanLP进行句子分割
     *
     * @param text 输入的大文本
     * @return 段落列表，每个段落至少包含minLength个字符
     */
    public static List<String> splitParagraphsHanLP(String text) {
        List<String> paragraphs = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return paragraphs;
        }

        // 1. 使用 HanLP 分词并分句
        List<String> sentences = splitSentences(text);

        // 2. 动态合并句子到段落
        paragraphs = mergeSentencesIntoParagraphs(sentences);

        return paragraphs;
    }

    /**
     * 句子级分割阶段：使用 HanLP 分词实现分句
     * <p>
     * 采用分词后逐词构建句子的设计，主要出于以下几个关键考量：
     * (1)标点歧义处理（核心原因）-中文存在标点复用场景：
     * 缩写中的句号：如"U.S.A."中的点不应触发分句
     * 情感符号："！"可能出现在句子中间（"太好了！！"）
     * 特殊格式：如"1. 项目背景"中的序号点 分词后的Term.word属性能精确判断标点是否独立成词，避免错误分割
     * (2)混合文本处理-实际文本常含中英文混杂：
     * 示例文本："请打开www.example.com。然后下载app！"
     * 分词结果：[请, 打开, www.example.com, 。, 然后, 下载, app, ！]
     * (3)扩展性考虑-后续处理可基于分词信息：
     * 过滤停用词
     * 实体识别
     * 语义分析-为后续NLP处理保留结构化数据
     */
    private static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder currentSentence = new StringBuilder();
        // 使用HanLP分词器进行基础分词
        List<Term> terms = HanLP.segment(text);

        for (Term term : terms) {
            currentSentence.append(term.word);
            // 通过标点符号识别句子边界（支持中英文标点）
            if (term.word.matches("[。！？；.!?;]+")) {
                // 逐词追加构建句子，遇到结束标点即完成一个句子
                sentences.add(currentSentence.toString());
                currentSentence.setLength(0);
            }
        }

        // 添加最后一个句子（如果没有标点结尾），保证文本末尾未闭合的内容也能作为独立句子
        if (!currentSentence.isEmpty()) {
            sentences.add(currentSentence.toString());
        }

        return sentences;
    }

    // 段落构建阶段：动态合并句子到段落
    private static List<String> mergeSentencesIntoParagraphs(List<String> sentences) {
        List<String> paragraphs = new ArrayList<>();
        StringBuilder currentParagraph = new StringBuilder();
        int currentLength = 0;

        for (String sentence : sentences) {
            int sentenceLength = countChineseChars(sentence);

            // 处理超长句子（强制分割）
            if (sentenceLength > TARGET_LENGTH + LENGTH_TOLERANCE) {
                if (currentLength > 0) {
                    paragraphs.add(currentParagraph.toString());
                    currentParagraph.setLength(0);
                    currentLength = 0;
                }
                // 按标点二次分割超长句
                List<String> subSentences = splitLongSentence(sentence);
                paragraphs.addAll(subSentences);
                continue;
            }

            // 合并到当前段落的条件
            if (currentLength + sentenceLength <= TARGET_LENGTH + LENGTH_TOLERANCE) {
                currentParagraph.append(sentence);
                currentLength += sentenceLength;
            } else {
                // 当前段落达到长度，保存并重置
                paragraphs.add(currentParagraph.toString());
                currentParagraph.setLength(0);
                currentParagraph.append(sentence);
                currentLength = sentenceLength;
            }
        }

        // 添加最后一个段落
        if (currentLength > 0) {
            paragraphs.add(currentParagraph.toString());
        }

        return paragraphs;
    }

    // 处理超长句子：按逗号、分号等二次分割
    private static List<String> splitLongSentence(String sentence) {
        List<String> validParts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentLength = 0;

        // 按标点分割句子
        String[] parts = sentence.split("[,;；，]");
        for (String part : parts) {
            int partLength = countChineseChars(part);
            if (currentLength + partLength > TARGET_LENGTH + LENGTH_TOLERANCE) {
                // 当前部分过长，保存并重置
                validParts.add(current.toString());
                current.setLength(0);
                currentLength = 0;
            }
            // 二次分割后自动补回中文逗号保持语句连贯性
            current.append(part).append("，");
            currentLength += partLength;
        }

        // 添加最后一个部分
        if (!current.isEmpty()) {
            validParts.add(current.toString());
        }

        return validParts;
    }

    // 统计中文字符数（忽略标点、英文）
    private static int countChineseChars(String text) {
        return (int) text.chars()
                .filter(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)
                .count();
    }

    /**
     * 文件内容提取
     *
     * @param file 上传的文件
     * @return 文件内容
     */
    public TikaVO extractText(MultipartFile file) {
        try {
            // 创建解析器--在不确定文档类型时候可以选择使用AutoDetectParser可以自动检测一个最合适的解析器
            Parser parser = new AutoDetectParser();
            // 用于捕获文档提取的文本内容。-1 参数表示使用无限缓冲区,解析到的内容通过此hander获取
            BodyContentHandler bodyContentHandler = new BodyContentHandler(-1);
            // 元数据对象，它在解析器中传递元数据属性---可以获取文档属性
            Metadata metadata = new Metadata();
            // 带有上下文相关信息的ParseContext实例，用于自定义解析过程。
            ParseContext parseContext = new ParseContext();
            parser.parse(file.getInputStream(), bodyContentHandler, metadata, parseContext);
            // 获取文本
            String text = bodyContentHandler.toString();
            // 元数据信息
            String[] names = metadata.names();
            // 将元数据转换成JSON字符串
            Map<String, String> map = new HashMap<>();
            for (String name : names) {
                map.put(name, metadata.get(name));
            }
            return splitParagraphs(text);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 使用langchain4j的分段工具
     *
     * @param content 文本内容
     */
    private TikaVO splitParagraphs(String content) {
        DocumentSplitter splitter = DocumentSplitters.recursive(TARGET_LENGTH, LENGTH_TOLERANCE, new OpenAiTokenizer());
        List<TextSegment> split = splitter.split(Document.document(content));
        return new TikaVO().setText(split.stream().map(TextSegment::text).toList()).setMetadata(split.stream().map(textSegment -> JSON.toJSONString(textSegment.metadata())).toList());
    }
}
