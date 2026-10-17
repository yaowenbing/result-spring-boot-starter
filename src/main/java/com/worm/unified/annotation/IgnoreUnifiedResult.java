package com.worm.unified.annotation;

import java.lang.annotation.Documented; // 注解文档化，生成 JavaDoc 时会包含此注解
import java.lang.annotation.ElementType; // 注解元素类型枚举，定义注解可标注的位置
import java.lang.annotation.Retention; // 注解保留策略，定义注解的生命周期
import java.lang.annotation.RetentionPolicy; // 保留策略枚举：SOURCE/CLASS/RUNTIME
import java.lang.annotation.Target; // 注解目标，定义注解可标注的程序元素

/**
 * 忽略统一返回结果包装注解
 * 标注在 Controller 方法上，表示该方法的返回值不需要被统一包装为 BaseResult
 * 
 * 使用场景：
 * 1. 文件下载接口，需要直接返回文件流
 * 2. 第三方回调接口，需要返回特定格式的数据
 * 3. 健康检查接口，需要返回简单的状态信息
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @IgnoreUnifiedResult
 * @GetMapping("/download")
 * public void download(HttpServletResponse response) {
 *     // 文件下载逻辑，不需要包装返回结果
 * }
 * }
 * </pre>
 *
 * @author wbyao
 */
@Target(ElementType.METHOD) // 指定此注解只能标注在方法上
@Retention(RetentionPolicy.RUNTIME) // 指定此注解在运行时保留，可通过反射获取
@Documented // 指定此注解会被包含在 JavaDoc 中
public @interface IgnoreUnifiedResult {
    // 此注解不包含任何属性，仅作为标记使用
}
