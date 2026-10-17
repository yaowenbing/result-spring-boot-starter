package com.worm.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseResultProperties 默认值测试。
 *
 * 为什么：默认值是消费方零配置接入的行为基线，尤其是过滤列表的默认内容直接决定哪些方法不被包装
 * —— 干什么：钉死三个开关与过滤列表的默认取值
 */
class BaseResultPropertiesTest {

    @Test
    @DisplayName("默认值：两个开关均启用，过滤列表为空")
    void defaults() {
        BaseResultProperties properties = new BaseResultProperties();

        assertThat(properties.isEnableUnifiedResult()).isTrue();
        assertThat(properties.isEnableUnifiedException()).isTrue();

        List<String> filterMethods = properties.getFilterUnifiedResultMethods();
        assertThat(filterMethods).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("setter：开关与过滤列表可覆盖")
    void settersOverrideDefaults() {
        BaseResultProperties properties = new BaseResultProperties();
        properties.setEnableUnifiedResult(false);
        properties.setEnableUnifiedException(false);
        properties.setFilterUnifiedResultMethods(java.util.Collections.singletonList("UserController.getUser"));

        assertThat(properties.isEnableUnifiedResult()).isFalse();
        assertThat(properties.isEnableUnifiedException()).isFalse();
        assertThat(properties.getFilterUnifiedResultMethods()).containsExactly("UserController.getUser");
    }
}
