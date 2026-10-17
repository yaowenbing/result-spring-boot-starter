package com.worm.unified.result;

import com.worm.unified.enums.EnumBaseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseResult 工厂方法测试。
 *
 * 为什么：统一返回体的 code / message / timestamp / data 契约被全站前端依赖，
 * 任一工厂方法的字段拼错都会造成前端解析失败 —— 干什么：逐工厂方法钉死字段取值与时间戳格式
 */
class BaseResultTest {

    /**
     * 时间戳格式：yyyy-MM-dd HH:mm:ss。
     */
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");

    @Test
    @DisplayName("ok() 无数据：code=200、message=OK、data=null、bizCode=null")
    void okWithoutData() {
        BaseResult<Object> result = BaseResult.ok();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo(EnumBaseResult.OK.getCode());
        assertThat(result.getData()).isNull();
        assertThat(result.getBizCode()).isNull();
        assertThat(result.getTimestamp()).matches(TIMESTAMP_PATTERN);
    }

    @Test
    @DisplayName("ok(data)：携带返回数据")
    void okWithData() {
        BaseResult<String> result = BaseResult.ok("payload");
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo(EnumBaseResult.OK.getCode());
        assertThat(result.getData()).isEqualTo("payload");
    }

    @Test
    @DisplayName("ok(message, data)：自定义成功消息")
    void okWithCustomMessage() {
        BaseResult<String> result = BaseResult.ok("created", "payload");
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("created");
        assertThat(result.getData()).isEqualTo("payload");
    }

    @Test
    @DisplayName("fail(data)：code=400、message=FAIL")
    void failWithData() {
        BaseResult<String> result = BaseResult.fail("boom");
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo(EnumBaseResult.FAIL.getCode());
        assertThat(result.getData()).isEqualTo("boom");
    }

    @Test
    @DisplayName("fail(code, data)：自定义状态码")
    void failWithCustomCode() {
        BaseResult<String> result = BaseResult.fail(409, "conflict");
        assertThat(result.getCode()).isEqualTo(409);
        assertThat(result.getMessage()).isEqualTo(EnumBaseResult.FAIL.getCode());
        assertThat(result.getData()).isEqualTo("conflict");
    }

    @Test
    @DisplayName("fail(message, data)：自定义失败消息")
    void failWithCustomMessage() {
        BaseResult<String> result = BaseResult.fail("bad request", "detail");
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("bad request");
        assertThat(result.getData()).isEqualTo("detail");
    }

    @Test
    @DisplayName("fail(code, message, data)：完全自定义")
    void failWithCodeAndMessage() {
        BaseResult<String> result = BaseResult.fail(451, "unavailable", "detail");
        assertThat(result.getCode()).isEqualTo(451);
        assertThat(result.getMessage()).isEqualTo("unavailable");
        assertThat(result.getData()).isEqualTo("detail");
    }

    @Test
    @DisplayName("unauthorized：code=401、message=UNAUTHORIZED")
    void unauthorized() {
        BaseResult<Object> result = BaseResult.unauthorized();
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo(EnumBaseResult.UNAUTHORIZED.getCode());
    }

    @Test
    @DisplayName("forbidden：code=403、message=FORBIDDEN")
    void forbidden() {
        BaseResult<Object> result = BaseResult.forbidden();
        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).isEqualTo(EnumBaseResult.FORBIDDEN.getCode());
    }

    @Test
    @DisplayName("notFound：code=404、message=NOT_FOUND")
    void notFound() {
        BaseResult<Object> result = BaseResult.notFound();
        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getMessage()).isEqualTo(EnumBaseResult.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("error：code=500、message=INTERNAL_ERROR")
    void error() {
        BaseResult<Object> result = BaseResult.error();
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo(EnumBaseResult.INTERNAL_ERROR.getCode());
    }

    @Test
    @DisplayName("custom：自定义状态码与消息")
    void custom() {
        BaseResult<String> result = BaseResult.custom(600, "custom", "data");
        assertThat(result.getCode()).isEqualTo(600);
        assertThat(result.getMessage()).isEqualTo("custom");
        assertThat(result.getData()).isEqualTo("data");
    }
}
