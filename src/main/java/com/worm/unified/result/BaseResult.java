package com.worm.unified.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worm.unified.enums.EnumBaseResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一返回结果类
 * 用于封装所有接口的返回数据，保证返回格式的一致性
 * <p>
 * 返回格式示例：
 * {
 * "code": 200,
 * "message": "OK",
 * "timestamp": "2026-03-25 10:30:00",
 * "data": { ... }
 * }
 *
 * @param <T> 返回数据的类型
 * @author winiger
 */
@Schema(description = "统一返回结果") // Swagger API 模型描述
@Data // Lombok 注解，自动生成 getter/setter/toString/equals/hashCode 方法
@AllArgsConstructor // Lombok 注解，自动生成全参构造函数
@NoArgsConstructor // Lombok 注解，自动生成无参构造函数
@Accessors(chain = true) // Lombok 注解，支持链式调用，如 result.setCode(200).setData(data)
public class BaseResult<T> {

    /**
     * 时间戳格式：年-月-日 时:分:秒。
     *
     * 为什么：统一返回体的 timestamp 字段为字符串，需固定格式便于前端解析 —— 干什么：以 DateTimeFormatter 钉死 yyyy-MM-dd HH:mm:ss
     */
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * JSON 序列化器。
     *
     * 为什么：String 类型返回值与 writeJson 需要把 BaseResult 序列化为 JSON 字符串，
     * 且 starter 不引入第三方 JSON 工具（保持零非 Spring 体系依赖） —— 干什么：持有静态不可变 ObjectMapper 实例复用
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * 返回代码
     * HTTP 状态码，如 200（成功）、400（失败）、401（未授权）等
     */
    @Schema(description = "返回代码") // Swagger 属性描述
    private int code; // HTTP 状态码字段

    /**
     * 业务状态码（字符串）
     * 用于携带更具体的业务语义码，如 "URL_OCCUPIED"，区别于 int 型的 HTTP 状态码。
     * 为可空字段，常规响应可不必设置。
     */
    @Schema(description = "业务状态码（字符串）") // Swagger 属性描述
    private String bizCode; // 业务状态码字段

    /**
     * 状态码说明
     * 用于描述返回状态的含义，如 "OK"、"FAIL"、"UNAUTHORIZED" 等
     */
    @Schema(description = "状态码说明") // Swagger 属性描述
    private String message; // 状态说明字段

    /**
     * 消息返回的具体时间
     * 格式：yyyy-MM-dd HH:mm:ss
     */
    @Schema(description = "消息返回的具体时间") // Swagger 属性描述
    private String timestamp; // 时间戳字段

    /**
     * 需要返回的数据
     * 泛型类型，可以是任意对象
     */
    @Schema(description = "需要返回的数据") // Swagger 属性描述
    private T data; // 返回数据字段

    // ==================== 成功相关方法 ====================

    /**
     * 返回成功结果（无数据）
     * 适用于不需要返回数据的成功场景
     *
     * @param <T> 返回数据的类型
     * @return 成功的统一返回结果，data 为 null
     */
    public static <T> BaseResult<T> ok() {
        return ok(null); // 调用 ok(T data) 方法，传入 null
    }

    /**
     * 返回成功结果
     * 适用于需要返回数据的成功场景
     *
     * @param data 返回的数据
     * @param <T>  返回数据的类型
     * @return 成功的统一返回结果
     */
    public static <T> BaseResult<T> ok(T data) {
        // 创建并返回成功结果：状态码 200，消息 "OK"，当前时间，返回数据
        // 为什么：构造成功结果，bizCode 留空（常规成功无需业务码） —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(HttpStatus.OK.value(), null, EnumBaseResult.OK.getCode(), now(), data);
    }

    /**
     * 返回成功结果（自定义消息）
     * 适用于需要自定义成功消息的场景
     *
     * @param message 自定义消息
     * @param data    返回的数据
     * @param <T>     返回数据的类型
     * @return 成功的统一返回结果
     */
    public static <T> BaseResult<T> ok(String message, T data) {
        // 创建并返回成功结果：状态码 200，自定义消息，当前时间，返回数据
        // 为什么：构造成功结果（自定义消息），bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(HttpStatus.OK.value(), null, message, now(), data);
    }

    // ==================== 失败相关方法 ====================

    /**
     * 返回失败结果
     * 适用于一般的失败场景
     *
     * @param data 错误数据，通常为错误信息
     * @param <T>  错误数据的类型
     * @return 失败的统一返回结果
     */
    public static <T> BaseResult<T> fail(T data) {
        // 创建并返回失败结果：状态码 400，消息 "FAIL"，当前时间，错误数据
        // 为什么：构造失败结果，bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(HttpStatus.BAD_REQUEST.value(), null, EnumBaseResult.FAIL.getCode(), now(), data);
    }

    /**
     * 返回失败结果（自定义状态码）
     * 适用于需要特定错误码的场景
     *
     * @param code 错误编码
     * @param data 错误数据
     * @param <T>  错误数据的类型
     * @return 失败的统一返回结果
     */
    public static <T> BaseResult<T> fail(int code, T data) {
        // 创建并返回失败结果：自定义状态码，消息 "FAIL"，当前时间，错误数据
        // 为什么：构造失败结果（自定义状态码），bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(code, null, EnumBaseResult.FAIL.getCode(), now(), data);
    }

    /**
     * 返回失败结果（自定义消息）
     * 适用于需要自定义错误消息的场景
     *
     * @param message 错误消息
     * @param data    错误数据
     * @param <T>     错误数据的类型
     * @return 失败的统一返回结果
     */
    public static <T> BaseResult<T> fail(String message, T data) {
        // 创建并返回失败结果：状态码 400，自定义消息，当前时间，错误数据
        // 为什么：构造失败结果（自定义消息），bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(HttpStatus.BAD_REQUEST.value(), null, message, now(), data);
    }

    /**
     * 返回失败结果（自定义状态码和消息）
     * 适用于需要完全自定义失败信息的场景
     *
     * @param code    错误编码
     * @param message 错误消息
     * @param data    错误数据
     * @param <T>     错误数据的类型
     * @return 失败的统一返回结果
     */
    public static <T> BaseResult<T> fail(int code, String message, T data) {
        // 创建并返回失败结果：自定义状态码，自定义消息，当前时间，错误数据
        // 为什么：构造结果，bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(code, null, message, now(), data);
    }

    // ==================== HTTP 状态码便捷方法 ====================

    /**
     * 返回 401 未授权（无数据）
     * 适用于用户未登录或令牌无效的场景
     *
     * @param <T> 返回数据的类型
     * @return 401 未授权的统一返回结果
     */
    public static <T> BaseResult<T> unauthorized() {
        return unauthorized(null); // 调用 unauthorized(T data) 方法，传入 null
    }

    /**
     * 返回 401 未授权
     * 适用于用户未登录或令牌无效的场景
     *
     * @param data 错误数据，如错误提示信息
     * @param <T>  返回数据的类型
     * @return 401 未授权的统一返回结果
     */
    public static <T> BaseResult<T> unauthorized(T data) {
        // 创建并返回 401 结果：状态码 401，消息 "UNAUTHORIZED"，当前时间，错误数据
        // 为什么：构造 401 结果，bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(HttpStatus.UNAUTHORIZED.value(), null, EnumBaseResult.UNAUTHORIZED.getCode(), now(), data);
    }

    /**
     * 返回 403 禁止访问（无数据）
     * 适用于用户无权限访问该资源的场景
     *
     * @param <T> 返回数据的类型
     * @return 403 禁止访问的统一返回结果
     */
    public static <T> BaseResult<T> forbidden() {
        return forbidden(null); // 调用 forbidden(T data) 方法，传入 null
    }

    /**
     * 返回 403 禁止访问
     * 适用于用户无权限访问该资源的场景
     *
     * @param data 错误数据，如错误提示信息
     * @param <T>  返回数据的类型
     * @return 403 禁止访问的统一返回结果
     */
    public static <T> BaseResult<T> forbidden(T data) {
        // 创建并返回 403 结果：状态码 403，消息 "FORBIDDEN"，当前时间，错误数据
        // 为什么：构造 403 结果，bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(HttpStatus.FORBIDDEN.value(), null, EnumBaseResult.FORBIDDEN.getCode(), now(), data);
    }

    /**
     * 返回 404 未找到（无数据）
     * 适用于请求的资源不存在的场景
     *
     * @param <T> 返回数据的类型
     * @return 404 未找到的统一返回结果
     */
    public static <T> BaseResult<T> notFound() {
        return notFound(null); // 调用 notFound(T data) 方法，传入 null
    }

    /**
     * 返回 404 未找到
     * 适用于请求的资源不存在的场景
     *
     * @param data 错误数据，如错误提示信息
     * @param <T>  返回数据的类型
     * @return 404 未找到的统一返回结果
     */
    public static <T> BaseResult<T> notFound(T data) {
        // 创建并返回 404 结果：状态码 404，消息 "NOT_FOUND"，当前时间，错误数据
        // 为什么：构造 404 结果，bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(HttpStatus.NOT_FOUND.value(), null, EnumBaseResult.NOT_FOUND.getCode(), now(), data);
    }

    /**
     * 返回 500 服务器错误（无数据）
     * 适用于服务器内部发生异常的场景
     *
     * @param <T> 返回数据的类型
     * @return 500 服务器错误的统一返回结果
     */
    public static <T> BaseResult<T> error() {
        return error(null); // 调用 error(T data) 方法，传入 null
    }

    /**
     * 返回 500 服务器错误
     * 适用于服务器内部发生异常的场景
     *
     * @param data 错误数据，如错误提示信息
     * @param <T>  返回数据的类型
     * @return 500 服务器错误的统一返回结果
     */
    public static <T> BaseResult<T> error(T data) {
        // 创建并返回 500 结果：状态码 500，消息 "INTERNAL_ERROR"，当前时间，错误数据
        // 为什么：构造 500 结果，bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), null, EnumBaseResult.INTERNAL_ERROR.getCode(), now(), data);
    }

    /**
     * 自定义返回结果
     * 适用于需要完全自定义返回信息的场景
     *
     * @param code    状态码
     * @param message 消息
     * @param data    数据
     * @param <T>     数据类型
     * @return 自定义的统一返回结果
     */
    public static <T> BaseResult<T> custom(int code, String message, T data) {
        // 创建并返回自定义结果：自定义状态码，自定义消息，当前时间，返回数据
        // 为什么：构造结果，bizCode 留空 —— 干什么：第 2 个参数为 bizCode，传 null
        return new BaseResult<>(code, null, message, now(), data);
    }

    // ==================== 工具方法 ====================

    /**
     * 输出 JSON 响应
     * 用于直接将统一返回结果写入 HTTP 响应
     *
     * @param response   HTTP 响应对象
     * @param baseResult 需要输出的统一返回结果
     * @param <T>        返回数据的类型
     */
    public static <T> void writeJson(HttpServletResponse response, BaseResult<T> baseResult) {
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name()); // 设置响应字符编码为 UTF-8
            response.setContentType(MediaType.APPLICATION_JSON_VALUE); // 设置响应内容类型为 JSON
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*"); // 设置跨域允许所有来源
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                    HttpMethod.POST.name() + "," + HttpMethod.GET.name()); // 设置跨域允许的方法
            response.getWriter().write(JSON_MAPPER.writeValueAsString(baseResult)); // 将结果写入响应
        } catch (Exception e) {
            // 为什么：writeJson 无 throws 声明（调用方多为过滤器/拦截器，不宜扩散受检异常），序列化失败只能以非受检异常上抛 —— 干什么：包装为 IllegalStateException 抛出
            throw new IllegalStateException("序列化统一返回结果失败", e);
        }
    }

    /**
     * 获取当前时间的格式化字符串。
     *
     * 为什么：统一返回体 timestamp 需固定格式 —— 干什么：以 TIMESTAMP_FORMATTER 格式化 LocalDateTime.now()
     *
     * @return yyyy-MM-dd HH:mm:ss 格式的当前时间
     */
    private static String now() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }
}
