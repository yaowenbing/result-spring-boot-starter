package com.worm.unified.exp;

import com.worm.unified.result.BaseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * BaseExceptionHandler 测试：各异常处理器返回的统一返回体。
 *
 * 为什么：全局异常处理是错误响应格式的唯一来源，处理器遗漏或状态码拼错会直接暴露给前端
 * —— 干什么：逐异常类型钉死 code / message / data 与 HTTP 状态
 */
class BaseExceptionHandlerTest {

    private final BaseExceptionHandler handler = new BaseExceptionHandler();

    @Test
    @DisplayName("BaseException：HTTP 200 + body 携带业务错误码与消息")
    void handleBaseException() {
        BaseException exp = new BaseException(4001, "库存不足");

        ResponseEntity<BaseResult<String>> response = handler.handleBaseException(exp);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        BaseResult<String> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(4001);
        assertThat(body.getData()).isEqualTo("库存不足");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException：返回字段级错误 Map")
    void handleMethodArgumentNotValidException() throws NoSuchMethodException {
        // 为什么：Spring 5.3 的 MethodArgumentNotValidException.getMessage() 会读取 MethodParameter，
        // 传 null 会在日志记录时 NPE —— 干什么：以真实方法参数构造异常，模拟 Spring MVC 的真实形态
        MethodParameter parameter = new MethodParameter(
                BaseExceptionHandlerTest.class.getDeclaredMethod("dummyTarget", String.class), 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "form");
        bindingResult.addError(new FieldError("form", "name", "不能为空"));
        MethodArgumentNotValidException exp = new MethodArgumentNotValidException(parameter, bindingResult);

        BaseResult<Map<String, String>> result = handler.handleMethodArgumentNotValidException(exp);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getData()).containsEntry("name", "不能为空");
    }

    /**
     * 仅供 MethodArgumentNotValidException 测试构造 MethodParameter 使用。
     */
    @SuppressWarnings("unused")
    private void dummyTarget(String name) {
    }

    @Test
    @DisplayName("BindException：返回字段级错误 Map")
    void handleBindException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "form");
        bindingResult.addError(new FieldError("form", "age", "必须为正数"));
        BindException exp = new BindException(bindingResult);

        BaseResult<Map<String, String>> result = handler.handleBindException(exp);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getData()).containsEntry("age", "必须为正数");
    }

    @Test
    @DisplayName("ConstraintViolationException：提取字段名（去方法名前缀）与消息")
    void handleConstraintViolationException() {
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("save.name");
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("不能为空");
        Set<ConstraintViolation<?>> violations = new HashSet<>(Collections.singletonList(violation));
        ConstraintViolationException exp = new ConstraintViolationException(violations);

        BaseResult<Map<String, String>> result = handler.handleConstraintViolationException(exp);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getData()).containsEntry("name", "不能为空");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException：提示缺少的参数名")
    void handleMissingServletRequestParameterException() {
        MissingServletRequestParameterException exp =
                new MissingServletRequestParameterException("token", "String");

        BaseResult<String> result = handler.handleMissingServletRequestParameterException(exp);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getData()).contains("token");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException：提示参数名与期望类型")
    void handleMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException exp =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "age", null, new NumberFormatException());

        BaseResult<String> result = handler.handleMethodArgumentTypeMismatchException(exp);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getData()).contains("age").contains("Integer");
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException：405")
    void handleHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException exp =
                new HttpRequestMethodNotSupportedException("PUT");

        BaseResult<String> result = handler.handleHttpRequestMethodNotSupportedException(exp);

        assertThat(result.getCode()).isEqualTo(405);
        assertThat(result.getData()).contains("PUT");
    }

    @Test
    @DisplayName("HttpMediaTypeNotSupportedException：415")
    void handleHttpMediaTypeNotSupportedException() {
        // 为什么：Spring 5.3 的 HttpMediaTypeNotSupportedException 仅提供 String / MimeType 构造器，
        // 且 String 构造后 getContentType() 返回 null（版本固有行为） —— 干什么：以 String 构造，
        // 仅断言状态码与消息前缀，不断言具体媒体类型
        HttpMediaTypeNotSupportedException exp =
                new HttpMediaTypeNotSupportedException("application/xml");

        BaseResult<String> result = handler.handleHttpMediaTypeNotSupportedException(exp);

        assertThat(result.getCode()).isEqualTo(415);
        assertThat(result.getData()).contains("不支持的媒体类型");
    }

    @Test
    @DisplayName("MaxUploadSizeExceededException：413")
    void handleMaxUploadSizeExceededException() {
        MaxUploadSizeExceededException exp = new MaxUploadSizeExceededException(1024);

        BaseResult<String> result = handler.handleMaxUploadSizeExceededException(exp);

        assertThat(result.getCode()).isEqualTo(413);
    }

    @Test
    @DisplayName("NoHandlerFoundException：404")
    void handleNoHandlerFoundException() {
        NoHandlerFoundException exp = new NoHandlerFoundException("GET", "/missing", null);

        BaseResult<String> result = handler.handleNoHandlerFoundException(exp);

        assertThat(result.getCode()).isEqualTo(404);
        assertThat(result.getData()).contains("/missing");
    }

    @Test
    @DisplayName("Throwable 兜底：500 并返回异常消息")
    void handleThrowable() {
        BaseResult<String> result = handler.handleThrowable(new RuntimeException("boom"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getData()).isEqualTo("boom");
    }

    @Test
    @DisplayName("Throwable 兜底：消息为空时返回默认文案")
    void handleThrowableWithNullMessage() {
        BaseResult<String> result = handler.handleThrowable(new RuntimeException());

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getData()).isEqualTo("系统内部错误");
    }
}
