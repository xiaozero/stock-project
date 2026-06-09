package com.xiao.stockproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    // 解决 history 路由刷新 404 问题
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 根路径
        registry.addViewController("/").setViewName("forward:/index.html");
        // 转发任意路径到 index.html，让 Vue Router 处理路由
        registry.addViewController("/{x:[\\w\\-]+}").setViewName("forward:/index.html");
    }
}