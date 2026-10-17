package com.worm.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * 统一返回结果配置属性类
 * 用于配置统一返回结果和异常处理的相关参数
 * 配置前缀: worm.result
 *
 * @author ywb
 */
@Data // Lombok 注解，自动生成 getter/setter/toString/equals/hashCode 方法
@ConfigurationProperties("worm.result") // 绑定配置文件中以 worm.result 开头的属性
public class BaseResultProperties {

    /**
     * 是否启用统一返回结果包装
     * 默认值: true（启用）
     * 可通过配置 worm.result.enable-unified-result=false 关闭
     */
    private boolean enableUnifiedResult = true; // 启用统一返回结果包装开关，默认为 true

    /**
     * 过滤不需要统一包装的方法列表
     * 格式: 类名.方法名（不区分大小写），且必须恰好包含一个 "."
     * 默认值: 空列表（不过滤任何方法）
     * 示例: - UserController.getUser
     * - OrderController.listOrders
     *
     * 为什么：原默认值 "CheckTokenEndpoint.checkToken" 是内部 OAuth2 授权服务器的特定端点，
     * 对通用开源 starter 属于无关的隐式行为 —— 干什么：默认改为空列表，需要的消费方自行配置
     */
    private List<String> filterUnifiedResultMethods = Collections.emptyList(); // 默认不过滤任何方法

    /**
     * 是否启用统一异常处理
     * 默认值: true（启用）
     * 可通过配置 worm.result.enable-unified-exception=false 关闭
     */
    private boolean enableUnifiedException = true; // 启用统一异常处理开关，默认为 true
}
