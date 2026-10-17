package com.worm.config;

import com.worm.StubObjectProvider;
import com.worm.properties.BaseResultProperties;
import com.worm.unified.exp.BaseExceptionHandler;
import com.worm.unified.result.BaseResultHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseResultAutoConfiguration 自动装配测试。
 *
 * 为什么：starter 的开关语义（matchIfMissing / ConditionalOnMissingBean）直接决定消费方
 * 零配置可用与自定义覆盖能力 —— 干什么：以 ApplicationContextRunner 轻量验证各开关与覆盖场景
 */
class BaseResultAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseResultAutoConfiguration.class));

    @Test
    @DisplayName("默认：两个处理器 Bean 均注册")
    void bothHandlersRegisteredByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(BaseResultHandler.class);
            assertThat(context).hasSingleBean(BaseExceptionHandler.class);
            assertThat(context).hasSingleBean(BaseResultProperties.class);
        });
    }

    @Test
    @DisplayName("enable-unified-result=false：仅结果处理器缺席")
    void resultHandlerDisabledByProperty() {
        contextRunner.withPropertyValues("worm.result.enable-unified-result=false").run(context -> {
            assertThat(context).doesNotHaveBean(BaseResultHandler.class);
            assertThat(context).hasSingleBean(BaseExceptionHandler.class);
        });
    }

    @Test
    @DisplayName("enable-unified-exception=false：仅异常处理器缺席")
    void exceptionHandlerDisabledByProperty() {
        contextRunner.withPropertyValues("worm.result.enable-unified-exception=false").run(context -> {
            assertThat(context).hasSingleBean(BaseResultHandler.class);
            assertThat(context).doesNotHaveBean(BaseExceptionHandler.class);
        });
    }

    @Test
    @DisplayName("自定义 Bean：@ConditionalOnMissingBean 不覆盖消费方实现")
    void customHandlerBeanTakesPrecedence() {
        contextRunner.withBean(BaseResultHandler.class, () -> new BaseResultHandler(
                        new BaseResultProperties(), new StubObjectProvider<>(null)))
                .run(context -> {
                    // 容器中仅存在消费方注册的 BaseResultHandler，自动配置不再创建第二个
                    assertThat(context).getBeanNames(BaseResultHandler.class).hasSize(1);
                    assertThat(context).hasSingleBean(BaseExceptionHandler.class);
                });
    }
}
