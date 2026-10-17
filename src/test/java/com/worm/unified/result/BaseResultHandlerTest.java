package com.worm.unified.result;

import com.worm.StubObjectProvider;
import com.worm.properties.BaseResultProperties;
import com.worm.unified.annotation.IgnoreUnifiedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseResultHandler 测试：supports 判定与 beforeBodyWrite 包装行为。
 *
 * 为什么：统一包装是全站响应格式的唯一入口，@IgnoreUnifiedResult / 过滤列表 / 排除类型
 * 任一判定失准都会导致包装缺失或错误包装 —— 干什么：以 MethodParameter 构造各返回类型场景逐条钉死行为
 */
class BaseResultHandlerTest {

    /**
     * 测试用 Controller：提供各返回类型与注解组合的方法。
     */
    static class DummyController {

        public String hello() {
            return "hello";
        }

        @IgnoreUnifiedResult
        public String ignored() {
            return "ignored";
        }

        public BaseResult<String> alreadyWrapped() {
            return BaseResult.ok("wrapped");
        }

        public ResponseEntity<String> responseEntity() {
            return ResponseEntity.ok("entity");
        }

        public void voidMethod() {
        }

        public List<String> listMethod() {
            return Collections.singletonList("item");
        }
    }

    private BaseResultHandler handler;

    private BaseResultHandler handlerWith(StaticMessageSource messageSource) {
        return new BaseResultHandler(new BaseResultProperties(), new StubObjectProvider<>(messageSource));
    }

    @BeforeEach
    void setUp() {
        handler = handlerWith(null);
    }

    private MethodParameter parameterOf(String methodName) throws NoSuchMethodException {
        return new MethodParameter(DummyController.class.getMethod(methodName), -1);
    }

    @Test
    @DisplayName("supports：普通返回值需要包装")
    void supportsPlainReturnType() throws NoSuchMethodException {
        assertThat(handler.supports(parameterOf("hello"), null)).isTrue();
    }

    @Test
    @DisplayName("supports：@IgnoreUnifiedResult 注解方法跳过包装")
    void supportsIgnoreAnnotatedMethod() throws NoSuchMethodException {
        assertThat(handler.supports(parameterOf("ignored"), null)).isFalse();
    }

    @Test
    @DisplayName("supports：BaseResult 与 ResponseEntity 返回类型跳过包装")
    void supportsExcludedReturnTypes() throws NoSuchMethodException {
        assertThat(handler.supports(parameterOf("alreadyWrapped"), null)).isFalse();
        assertThat(handler.supports(parameterOf("responseEntity"), null)).isFalse();
    }

    @Test
    @DisplayName("supports：过滤列表中的方法跳过包装（不区分大小写）")
    void supportsFilteredMethod() throws NoSuchMethodException {
        BaseResultProperties properties = new BaseResultProperties();
        properties.setFilterUnifiedResultMethods(Collections.singletonList("dummycontroller.hello"));
        BaseResultHandler filteredHandler = new BaseResultHandler(properties, new StubObjectProvider<>(null));

        assertThat(filteredHandler.supports(parameterOf("hello"), null)).isFalse();
        // 未被过滤的方法不受影响
        assertThat(filteredHandler.supports(parameterOf("listMethod"), null)).isTrue();
    }

    @Test
    @DisplayName("supports：格式非法的过滤规则（无恰好一个点）不生效")
    void supportsMalformedFilterRuleIgnored() throws NoSuchMethodException {
        BaseResultProperties properties = new BaseResultProperties();
        properties.setFilterUnifiedResultMethods(java.util.Arrays.asList("DummyController", "a.b.c", "DummyController."));
        BaseResultHandler filteredHandler = new BaseResultHandler(properties, new StubObjectProvider<>(null));

        assertThat(filteredHandler.supports(parameterOf("hello"), null)).isTrue();
    }

    @Test
    @DisplayName("beforeBodyWrite：void 返回空成功结果")
    void beforeBodyWriteVoid() throws NoSuchMethodException {
        Object written = handler.beforeBodyWrite(null, parameterOf("voidMethod"), null, null, null, null);

        assertThat(written).isInstanceOf(BaseResult.class);
        BaseResult<?> result = (BaseResult<?>) written;
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("beforeBodyWrite：BaseResult 返回值原样透传")
    void beforeBodyWritePassThroughBaseResult() throws NoSuchMethodException {
        BaseResult<String> original = BaseResult.ok("wrapped");
        Object written = handler.beforeBodyWrite(original, parameterOf("alreadyWrapped"), null, null, null, null);

        assertThat(written).isSameAs(original);
    }

    @Test
    @DisplayName("beforeBodyWrite：普通对象包装为成功结果")
    void beforeBodyWritePlainObject() throws NoSuchMethodException {
        List<String> body = Collections.singletonList("item");
        Object written = handler.beforeBodyWrite(body, parameterOf("listMethod"), null, null, null, null);

        assertThat(written).isInstanceOf(BaseResult.class);
        BaseResult<?> result = (BaseResult<?>) written;
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isSameAs(body);
    }

    @Test
    @DisplayName("beforeBodyWrite：String 返回值序列化为 JSON 字符串（含国际化）")
    void beforeBodyWriteStringSerializedToJson() throws NoSuchMethodException {
        StaticMessageSource messageSource = new StaticMessageSource();
        messageSource.addMessage("greeting", java.util.Locale.getDefault(), "Hello!");
        BaseResultHandler i18nHandler = handlerWith(messageSource);

        Object written = i18nHandler.beforeBodyWrite("greeting", parameterOf("hello"), null, null, null, null);

        assertThat(written).isInstanceOf(String.class);
        String json = (String) written;
        assertThat(json).contains("\"code\":200");
        assertThat(json).contains("\"data\":\"Hello!\"");
    }

    @Test
    @DisplayName("beforeBodyWrite：String 返回值在消息源缺失时退化为原始值")
    void beforeBodyWriteStringWithoutMessageSource() throws NoSuchMethodException {
        Object written = handler.beforeBodyWrite("greeting", parameterOf("hello"), null, null, null, null);

        assertThat(written).isInstanceOf(String.class);
        assertThat((String) written).contains("\"data\":\"greeting\"");
    }

    @Test
    @DisplayName("beforeBodyWrite：String 返回值查找不到消息时退化为原始值")
    void beforeBodyWriteStringUnknownMessageFallsBack() throws NoSuchMethodException {
        StaticMessageSource messageSource = new StaticMessageSource();
        BaseResultHandler i18nHandler = handlerWith(messageSource);

        Object written = i18nHandler.beforeBodyWrite("unknown.code", parameterOf("hello"), null, null, null, null);

        assertThat(written).isInstanceOf(String.class);
        assertThat((String) written).contains("\"data\":\"unknown.code\"");
    }
}
