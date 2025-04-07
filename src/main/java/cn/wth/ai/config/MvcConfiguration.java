package cn.wth.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author: 5th
 * @Description: 跨域配置类
 * @CreateTime: 2025-04-07 20:46
 */
@Configuration
// 配置类，实现WebMvcConfigurer接口以自定义MVC配置
public class MvcConfiguration implements WebMvcConfigurer {

    @Override
    // 重写addCorsMappings方法，配置跨域资源共享规则
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                                  // 配置路径匹配
                .allowedOrigins("*")                                          // 允许所有来源域
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")    // 允许的HTTP方法
                .allowedHeaders("*")                                          // 允许所有请求头
                .exposedHeaders("Content-Disposition");                       // 暴露特定响应头
    }

}
