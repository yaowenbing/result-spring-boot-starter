package com.worm.unified.exp;

import com.worm.unified.result.BaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局统一异常处理器
 * 拦截所有 Controller 层抛出的异常，统一包装为 BaseResult 格式返回
 * <p>
 * 处理的异常类型包括：
 * 1. 自定义异常（BaseException）
 * 2. 参数校验异常（@Valid、@Validated）
 * 3. HTTP 相关异常（404、405、415 等）
 * 4. 其他未知异常
 *
 * @author ywb
 */
@Slf4j // Lombok 注解，自动生成名为 log 的 SLF4J 日志对象
@ControllerAdvice // Spring 注解，标识这是一个控制器增强类，用于全局异常处理
@ResponseBody // Spring 注解，标识返回值直接写入响应体
public class BaseExceptionHandler {
    /**
     * 处理异常的基础方法
     * 记录错误日志并返回失败的统一结果
     *
     * @param exp  异常对象
     * @param data 错误数据
     * @param <T>  数据类型
     * @return 失败的统一返回结果
     */
    private <T> BaseResult<T> handleException(Exception exp, T data) {
        log.error("发生异常：{}", exp.getMessage(), exp); // 记录错误日志，包含异常信息和堆栈
        return BaseResult.fail(data); // 返回失败的统一结果
    }

    /**
     * 处理异常的基础方法（自定义状态码）
     * 记录错误日志并返回指定状态码的失败结果
     *
     * @param exp  异常对象
     * @param code 错误状态码
     * @param data 错误数据
     * @param <T>  数据类型
     * @return 失败的统一返回结果
     */
    private <T> BaseResult<T> handleException(Exception exp, int code, T data) {
        log.error("发生异常：{}", exp.getMessage(), exp); // 记录错误日志，包含异常信息和堆栈
        return BaseResult.fail(code, data); // 返回指定状态码的失败结果
    }

    // ==================== 自定义异常处理 ====================

    /**
     * 处理自定义统一异常
     * 用于业务层主动抛出的异常，支持自定义错误码
     *
     * @param exp 自定义异常对象
     * @return 失败的统一返回结果，包含自定义错误码和消息
     */
    @ExceptionHandler(BaseException.class) // 指定处理 BaseException 类型的异常
    public ResponseEntity<BaseResult<String>> handleBaseException(BaseException exp) {
        log.error("业务异常：错误码={}，错误信息={}", exp.getCode(), exp.getMessage()); // 记录业务异常日志
        return ResponseEntity
                .ok(BaseResult.fail(exp.getCode(), exp.getMessage())); // HTTP 200 + body 携带业务错误码，避免非标准 HTTP 状态码导致代理/客户端解析失败
    }

    // ==================== 参数校验异常处理 ====================

    /**
     * 处理 @RequestBody 参数校验异常
     * 当使用 @Valid 或 @Validated 注解校验 JSON 请求体失败时触发
     *
     * @param exp 方法参数校验异常对象
     * @return 失败的统一返回结果，data 中包含字段名和错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class) // 指定处理 @RequestBody 校验异常
    public BaseResult<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exp) {
        BindingResult bindingResult = exp.getBindingResult(); // 获取绑定结果
        Map<String, String> errorMap = buildErrorMap(bindingResult); // 构建错误信息 Map
        return handleException(exp, errorMap); // 返回包含错误信息的失败结果
    }

    /**
     * 处理表单绑定异常
     * 当使用 @Valid 或 @Validated 注解校验表单参数失败时触发
     *
     * @param exp 表单绑定异常对象
     * @return 失败的统一返回结果，data 中包含字段名和错误信息
     */
    @ExceptionHandler(BindException.class) // 指定处理表单绑定异常
    public BaseResult<Map<String, String>> handleBindException(BindException exp) {
        Map<String, String> errorMap = buildErrorMap(exp.getBindingResult()); // 构建错误信息 Map
        return handleException(exp, errorMap); // 返回包含错误信息的失败结果
    }

    /**
     * 处理 JSR-303 约束违反异常
     * 当使用 @Validated 注解校验方法参数（非 @RequestBody）失败时触发
     *
     * @param exp 约束违反异常对象
     * @return 失败的统一返回结果，data 中包含字段名和错误信息
     */
    @ExceptionHandler(ConstraintViolationException.class) // 指定处理 JSR-303 约束违反异常
    public BaseResult<Map<String, String>> handleConstraintViolationException(ConstraintViolationException exp) {
        Map<String, String> errorMap = new HashMap<>(); // 创建错误信息 Map
        // 遍历所有约束违反
        for (ConstraintViolation<?> violation : exp.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString(); // 获取属性路径（如方法名.参数名）
            // 提取字段名（去掉方法名前缀）
            String fieldName = propertyPath.contains(".") ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1) : propertyPath;
            errorMap.put(fieldName, violation.getMessage()); // 添加字段名和错误信息
        }
        return handleException(exp, errorMap); // 返回包含错误信息的失败结果
    }

    /**
     * 处理缺少必要请求参数异常
     * 当请求缺少 @RequestParam(required=true) 参数时触发
     *
     * @param exp 缺少请求参数异常对象
     * @return 失败的统一返回结果，提示缺少的参数名
     */
    @ExceptionHandler(MissingServletRequestParameterException.class) // 指定处理缺少参数异常
    public BaseResult<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException exp) {
        String message = "缺少必要参数：" + exp.getParameterName(); // 构建错误消息
        return handleException(exp, message); // 返回包含错误消息的失败结果
    }

    /**
     * 处理参数类型不匹配异常
     * 当请求参数类型与方法参数类型不匹配时触发（如字符串转数字失败）
     *
     * @param exp 参数类型不匹配异常对象
     * @return 失败的统一返回结果，提示参数类型错误
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class) // 指定处理参数类型不匹配异常
    public BaseResult<String> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException exp) {
        String message = "参数类型错误：" + exp.getName() + " 应为 " + exp.getRequiredType(); // 构建错误消息
        return handleException(exp, message); // 返回包含错误消息的失败结果
    }

    // ==================== HTTP 相关异常处理 ====================

    /**
     * 处理 404 异常
     * 当请求的 URL 没有对应的处理器时触发
     * 注意：需要在配置中设置 spring.mvc.throw-exception-if-no-handler-found=true
     *
     * @param exp 处理器未找到异常对象
     * @return 失败的统一返回结果，状态码 404
     */
    @ExceptionHandler(NoHandlerFoundException.class) // 指定处理 404 异常
    public BaseResult<String> handleNoHandlerFoundException(NoHandlerFoundException exp) {
        String message = "请求的资源不存在：" + exp.getRequestURL(); // 构建错误消息
        return handleException(exp, HttpStatus.NOT_FOUND.value(), message); // 返回 404 状态的失败结果
    }

    /**
     * 处理请求方法不支持异常
     * 当请求方法与处理器支持的方法不匹配时触发（如 POST 请求访问 GET 接口）
     *
     * @param exp 请求方法不支持异常对象
     * @return 失败的统一返回结果，状态码 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class) // 指定处理请求方法不支持异常
    public BaseResult<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exp) {
        // 构建错误消息
        String[] supportedMethods = exp.getSupportedMethods();
        String message = "不支持的请求方法：" + exp.getMethod() + (supportedMethods != null ? "，支持的方法：" + String.join("、", supportedMethods) : "");
        // 返回 405 状态的失败结果
        return handleException(exp, HttpStatus.METHOD_NOT_ALLOWED.value(), message);
    }

    /**
     * 处理媒体类型不支持异常
     * 当请求的 Content-Type 与处理器支持的不匹配时触发
     *
     * @param exp 媒体类型不支持异常对象
     * @return 失败的统一返回结果，状态码 415
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class) // 指定处理媒体类型不支持异常
    public BaseResult<String> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException exp) {
        // 构建错误消息
        String message = "不支持的媒体类型：" + exp.getContentType();
        // 返回 415 状态的失败结果
        return handleException(exp, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), message);
    }

    /**
     * 处理上传文件大小超出限制异常
     * 当上传文件大小超出 Spring 配置的限制时触发
     *
     * @param exp 上传文件大小超出限制异常对象
     * @return 失败的统一返回结果，状态码 413
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResult<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exp) {
        log.info("上传文件大小超出限制：{}", exp.getMessage());
        String message = "上传文件大小超出限制，请确认单个文件最大允许多少，请求总大小最大允许多少";
        return handleException(exp, HttpStatus.PAYLOAD_TOO_LARGE.value(), message);
    }


    // ==================== 通用异常处理 ====================

    /**
     * 处理所有未捕获的异常
     * 作为兜底处理器，捕获所有未被其他处理器处理的异常
     *
     * @param exp 异常对象
     * @return 失败的统一返回结果，状态码 500
     */
    @ExceptionHandler(Throwable.class) // 指定处理所有 Throwable 类型异常
    public BaseResult<String> handleThrowable(Throwable exp) {
        // 记录错误日志
        log.error("发生未知异常：{}", exp.getMessage(), exp);
        // 获取错误消息，如果为空则使用默认消息
        String message = exp.getMessage() != null ? exp.getMessage() : "系统内部错误";
        // 返回 500 状态的失败结果
        return BaseResult.error(message);
    }

    /**
     * 构建错误信息 Map
     * 从绑定结果中提取字段名和错误信息
     *
     * @param bindingResult 绑定结果对象
     * @return 字段名 -> 错误信息的 Map
     */
    private Map<String, String> buildErrorMap(BindingResult bindingResult) {
        // 如果没有错误，返回空 Map
        if (!bindingResult.hasErrors()) {
            return Collections.emptyMap(); // 返回不可变的空 Map
        }
        // 创建错误信息 Map，初始容量为错误字段数量
        // 为什么：JDK 8 无 HashMap.newHashMap（Java 19+ API），须以构造器指定初始容量达到同等效果 —— 干什么：以 new HashMap<>(count) 预分配容量
        Map<String, String> errorMap = new HashMap<>(bindingResult.getFieldErrorCount());
        // 获取所有字段错误
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        // 遍历字段错误，添加到 Map 中
        fieldErrors.forEach(fieldError -> errorMap.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return errorMap; // 返回错误信息 Map
    }
}
