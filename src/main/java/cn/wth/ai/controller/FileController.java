package cn.wth.ai.controller;

import cn.wth.ai.utils.TikaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Author: 5th
 * @Description: 文件处理
 * @CreateTime: 2025-04-09 11:09
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/file")
public class FileController {

    private final TikaUtil tikaUtil;

    @PostMapping("/extractFileString")
    public ResponseEntity<String> extractFileString(MultipartFile file) {
        return ResponseEntity.ok(tikaUtil.extractTextString(file));
    }

    // 解析文件内容-HanLP分片
    @PostMapping("/splitParagraphsHanLP")
    public ResponseEntity<List<String>> splitParagraphsHanLP(MultipartFile file) {
        return ResponseEntity.ok(tikaUtil.splitParagraphsHanLP(tikaUtil.extractTextString(file)));
    }



}
