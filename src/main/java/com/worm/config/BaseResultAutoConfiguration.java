package com.worm.config;

import com.worm.properties.BaseResultProperties;
import com.worm.unified.exp.BaseExceptionHandler;
import com.worm.unified.result.BaseResultHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一返回结果自动配置类
 * Spring Boot Starter 的核心配置类，用于自动配置统一返回结果和异常处理功能
 * <p>
 * 配置项：
 * - worm.result.enable-unified-result: 是否启用统一返回结果包装（默认 true）
 * - worm.result.enable-unified-exception: 是否启用统一异常处理（默认 true）
 *
 * @author ywb
 */
@Configuration // Spring 注解，标识这是一个配置类
@EnableConfigurationProperties(BaseResultProperties.class) // 启用 BaseResultProperties 配置属性绑定
public class BaseResultAutoConfiguration {

    /**
     * 统一返回结果配置属性
     */
    private final BaseResultProperties baseResultProperties; // 配置属性对象

    /**
     * 国际化消息源提供者（可选 Bean）。
     *
     * 为什么：MessageSource 在无 Web 环境或精简容器中可能缺失，字段注入（@Resource）会因 Bean 不存在导致容器启动失败
     * —— 干什么：经 ObjectProvider 构造注入，缺失时 BaseResultHandler 内部退化为不使用国际化
     */
    private final ObjectProvider<MessageSource> messageSourceProvider; // 国际化消息源提供者

    /**
     * 构造函数
     *
     * @param baseResultProperties  统一返回结果配置属性
     * @param messageSourceProvider 国际化消息源提供者
     */
    public BaseResultAutoConfiguration(BaseResultProperties baseResultProperties, ObjectProvider<MessageSource> messageSourceProvider) {
        this.baseResultProperties = baseResultProperties; // 初始化配置属性
        this.messageSourceProvider = messageSourceProvider; // 初始化消息源提供者
    }

    /**
     * 注册统一返回结果处理器
     * 拦截 Controller 方法的返回值，统一包装为 BaseResult 格式
     * <p>
     * 条件说明：
     * - @ConditionalOnMissingBean: 当容器中不存在 BaseResultHandler Bean 时才创建
     * - @ConditionalOnProperty: 当配置项 worm.result.enable-unified-result=true 时才创建（默认 true）
     *
     * @return 统一返回结果处理器实例
     */
    @Bean // Spring 注解，标识这是一个 Bean 定义方法
    @ConditionalOnMissingBean(BaseResultHandler.class) // 当容器中不存在 BaseResultHandler 时生效
    @ConditionalOnProperty(prefix = "worm.result", name = "enable-unified-result", havingValue = "true", matchIfMissing = true) // 当配置项为 true 或不存在时生效
    public BaseResultHandler unifiedResultHandler() {
        // 创建并返回统一返回结果处理器，传入配置属性与消息源提供者
        return new BaseResultHandler(baseResultProperties, messageSourceProvider);
    }

    /**
     * 注册全局异常处理器
     * 捕获所有 Controller 层抛出的异常，统一包装为 BaseResult 格式
     * <p>
     * 条件说明：
     * - @ConditionalOnMissingBean: 当容器中不存在 BaseExceptionHandler Bean 时才创建
     * - @ConditionalOnProperty: 当配置项 worm.result.enable-unified-exception=true 时才创建（默认 true）
     *
     * @return 全局异常处理器实例
     */
    @Bean // Spring 注解，标识这是一个 Bean 定义方法
    @ConditionalOnMissingBean(BaseExceptionHandler.class) // 当容器中不存在 BaseExceptionHandler 时生效
    @ConditionalOnProperty(prefix = "worm.result", name = "enable-unified-exception", havingValue = "true", matchIfMissing = true) // 当配置项为 true 或不存在时生效
    public BaseExceptionHandler unifiedExceptionHandler() {
        // 创建并返回全局异常处理器
        return new BaseExceptionHandler();
    }
}
