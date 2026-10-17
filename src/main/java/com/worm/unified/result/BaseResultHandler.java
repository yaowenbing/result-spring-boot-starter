package com.worm.unified.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worm.properties.BaseResultProperties;
import com.worm.unified.annotation.IgnoreUnifiedResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 统一返回结果处理器
 * 实现 ResponseBodyAdvice 接口，拦截 Controller 方法的返回值，统一包装为 BaseResult 格式
 * <p>
 * 功能说明：
 * 1. 自动将 Controller 返回值包装为 BaseResult 格式
 * 2. 支持通过配置过滤特定方法
 * 3. 支持通过 @IgnoreUnifiedResult 注解跳过包装
 * 4. 支持 String 类型返回值的国际化处理
 *
 * @author ywb
 */
@Slf4j // Lombok 注解，自动生成名为 log 的 SLF4J 日志对象
@ControllerAdvice // Spring 注解，标识这是一个控制器增强类
public class BaseResultHandler implements ResponseBodyAdvice<Object> {

    /**
     * JSON 序列化器。
     *
     * 为什么：String 类型返回值需先序列化为 JSON 字符串再交还 StringHttpMessageConverter，
     * 且 starter 不引入第三方 JSON 工具（保持零非 Spring 体系依赖） —— 干什么：持有静态不可变 ObjectMapper 实例复用
     */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * 不需要包装的返回类型列表
     * BaseResult 和 ResponseEntity 类型不需要再次包装
     *
     * 为什么：BaseResult 已是目标格式、ResponseEntity 由调用方完全掌控响应，二次包装会破坏语义 —— 干什么：以不可变列表固化排除类型
     */
    private static final List<Class<?>> EXCLUDED_TYPES = Collections.unmodifiableList(
            Arrays.asList(BaseResult.class, ResponseEntity.class)); // 排除类型列表

    /**
     * 配置属性对象
     * 包含启用开关和过滤方法列表
     */
    private final BaseResultProperties baseResultProperties; // 配置属性

    /**
     * 国际化消息源（可选 Bean）。
     *
     * 为什么：MessageSource 虽由 Spring Boot 自动配置提供，但在无 Web 环境或精简容器中可能缺失，
     * 字段注入（@Resource）会因 Bean 不存在导致容器启动失败 —— 干什么：经 ObjectProvider 延迟获取，
     * 缺失时 String 返回值退化为使用原始值，不影响统一包装主流程。
     */
    private final ObjectProvider<MessageSource> messageSourceProvider; // 国际化消息源提供者

    /**
     * 构造函数
     *
     * @param baseResultProperties   统一返回结果配置属性
     * @param messageSourceProvider  国际化消息源提供者（可为空提供者，缺失时跳过国际化）
     */
    public BaseResultHandler(BaseResultProperties baseResultProperties, ObjectProvider<MessageSource> messageSourceProvider) {
        this.baseResultProperties = baseResultProperties; // 初始化配置属性
        this.messageSourceProvider = messageSourceProvider; // 初始化消息源提供者
    }

    /**
     * 判断是否需要包装返回结果
     * 在响应体写入之前调用，决定是否执行 beforeBodyWrite 方法
     *
     * @param returnType    方法返回类型封装
     * @param converterType 消息转换器类型
     * @return true-需要包装，false-不需要包装
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查方法是否有 @IgnoreUnifiedResult 注解
        Method method = returnType.getMethod(); // 获取方法对象
        if (method != null && method.isAnnotationPresent(IgnoreUnifiedResult.class)) {
            log.debug("方法 [{}] 标注了 @IgnoreUnifiedResult 注解，跳过统一包装", method.getName()); // 记录调试日志
            return false; // 不需要包装
        }

        // 获取类名和方法名
        String simpleName = returnType.getExecutable().getDeclaringClass().getSimpleName(); // 获取类名（不含包名）
        String methodName = returnType.getExecutable().getName(); // 获取方法名

        // 检查是否在配置的过滤列表中
        boolean isFiltered = isFilteredMethod(simpleName, methodName); // 判断是否被过滤
        if (isFiltered) {
            log.debug("方法 [{}.{}] 在过滤列表中，跳过统一包装", simpleName, methodName); // 记录调试日志
            return false; // 不需要包装
        }

        // 检查返回类型是否需要包装
        Class<?> parameterType = returnType.getParameterType(); // 获取返回类型
        boolean needWrap = !EXCLUDED_TYPES.contains(parameterType); // 判断是否不在排除列表中

        log.debug("检查统一包装：类={}, 方法={}, 类型={}, 需要包装={}",
                simpleName, methodName, parameterType.getSimpleName(), needWrap); // 记录调试日志

        return needWrap; // 返回是否需要包装
    }

    /**
     * 包装返回结果
     * 在响应体写入之前调用，将原始返回值包装为 BaseResult 格式
     *
     * @param body                  原始返回值
     * @param returnType            方法返回类型封装
     * @param selectedContentType   选中的内容类型
     * @param selectedConverterType 选中的消息转换器类型
     * @param request               服务端请求对象
     * @param response              服务端响应对象
     * @return 包装后的返回结果
     */
    @Override
    public Object beforeBodyWrite(@Nullable Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        Class<?> type = returnType.getParameterType(); // 获取返回类型
        BaseResult<Object> result = BaseResult.ok(); // 创建成功结果对象

        // void 类型返回空成功结果
        if (void.class == type) {
            log.debug("void 类型方法，返回空成功结果"); // 记录调试日志
            return result; // 返回空成功结果
        }

        // 已经是 BaseResult 类型，直接返回
        if (BaseResult.class == type) {
            log.debug("返回值已是 BaseResult 类型，直接返回"); // 记录调试日志
            return body; // 直接返回原始值
        }

        // String 类型特殊处理（支持国际化）
        // 为什么：Controller 声明返回 String 时，Spring 已选定 StringHttpMessageConverter，
        // 该转换器会把响应体当作 String 处理；若此处返回 BaseResult 对象，则会在
        // StringHttpMessageConverter 处触发 ClassCastException（BaseResult cannot be cast to String）。
        // 干什么：把包装后的 BaseResult 序列化为 JSON 字符串再返回，让 StringHttpMessageConverter
        // 直接写出该字符串，前端仍收到 {"code":200,"data":"...",...} 结构。
        if (String.class == type) {
            String message = resolveI18nMessage(body); // 解析国际化消息（消息源缺失时退化为原始值）
            result.setData(message); // 设置国际化消息为返回数据
            log.debug("String 类型返回值，已进行国际化处理"); // 记录调试日志
            return toJsonString(result); // 序列化为 JSON 字符串，避免 StringHttpMessageConverter 类型转换异常
        }

        // 其他类型直接包装
        result.setData(body); // 设置原始返回值为返回数据
        log.debug("返回值已包装为统一格式，类型：{}", type.getSimpleName()); // 记录调试日志
        return result; // 返回包装后的结果
    }

    /**
     * 解析 String 返回值的国际化消息。
     *
     * 为什么：消息源是可选依赖，缺失时不应因国际化功能影响主流程 —— 干什么：消息源可用时按当前 Locale 查找消息，
     * 查找异常或消息源缺失时回退为原始值。
     *
     * @param body 原始返回值（String 或 null）
     * @return 国际化后的消息；body 为 null 时返回 null
     */
    @Nullable
    private String resolveI18nMessage(@Nullable Object body) {
        if (body == null) {
            return null; // 无返回值时无需国际化
        }
        MessageSource messageSource = messageSourceProvider.getIfAvailable(); // 延迟获取消息源
        if (messageSource == null) {
            log.debug("容器中无 MessageSource，跳过国际化，使用原始值：{}", body); // 记录调试日志
            return (String) body; // 使用原始值
        }
        try {
            // 尝试从消息源获取国际化消息
            return messageSource.getMessage((String) body, null, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            log.debug("国际化消息查找失败，使用原始值：{}", body); // 记录调试日志
            return (String) body; // 使用原始值
        }
    }

    /**
     * 将统一返回结果序列化为 JSON 字符串。
     *
     * 为什么：beforeBodyWrite 无 throws 声明，序列化失败只能以非受检异常上抛 —— 干什么：包装为 IllegalStateException
     *
     * @param result 统一返回结果
     * @return JSON 字符串
     */
    private String toJsonString(BaseResult<Object> result) {
        try {
            return JSON_MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            throw new IllegalStateException("序列化统一返回结果失败", e);
        }
    }

    /**
     * 判断方法是否在配置的过滤列表中
     * 过滤列表中的方法不会被统一包装
     *
     * @param simpleName 类名（不含包名）
     * @param methodName 方法名
     * @return true-在过滤列表中，false-不在过滤列表中
     */
    private boolean isFilteredMethod(String simpleName, String methodName) {
        List<String> filterMethods = baseResultProperties.getFilterUnifiedResultMethods(); // 获取过滤方法列表
        // 如果过滤列表为空，返回 false
        if (filterMethods == null || filterMethods.isEmpty()) {
            return false; // 不在过滤列表中
        }

        // 遍历过滤列表，检查是否匹配
        return filterMethods.stream().anyMatch(filterMethod -> {
            // 为什么：过滤规则为 "类名.方法名"，须拆分为两段后分别比较；不含恰好一个 "." 的规则视为非法格式直接不匹配
            // 干什么：以唯一 "." 为分隔符拆分类名与方法名
            int dotIndex = filterMethod.indexOf('.');
            if (dotIndex < 0 || dotIndex != filterMethod.lastIndexOf('.')) {
                return false; // 格式不正确，不匹配
            }
            String filterClass = filterMethod.substring(0, dotIndex); // 规则中的类名
            String filterMethodName = filterMethod.substring(dotIndex + 1); // 规则中的方法名
            // 比较类名和方法名（不区分大小写）
            return filterClass.equalsIgnoreCase(simpleName) && filterMethodName.equalsIgnoreCase(methodName); // 返回是否匹配
        });
    }
}
