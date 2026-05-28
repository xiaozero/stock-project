package com.xiao.stockproject.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xiaohuijia
 * @date 2025/8/8 10:20
 */
@Configuration
public class MybatisPlusConfig {
    public static ThreadLocal<String> TABLE_NAME = new ThreadLocal<>();
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        //分页拦截器
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setOverflow(false);
        paginationInnerInterceptor.setMaxLimit(500l);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);


        return interceptor;
    }
}
