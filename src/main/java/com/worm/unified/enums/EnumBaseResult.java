package com.worm.unified.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一返回状态枚举类
 * 定义了系统返回结果的状态码和描述信息
 * 用于统一返回结果的 message 字段
 *
 * @author wbyao
 */
@Getter // Lombok 注解，自动生成 getter 方法
@AllArgsConstructor // Lombok 注解，自动生成包含所有参数的构造函数
public enum EnumBaseResult {

    /**
     * 成功状态
     * 用于表示操作成功完成
     */
    OK("OK", "成功"), // 成功状态码和描述

    /**
     * 失败状态
     * 用于表示操作失败或业务异常
     */
    FAIL("FAIL", "失败"), // 失败状态码和描述

    /**
     * 未授权状态
     * 用于表示用户未登录或令牌无效
     */
    UNAUTHORIZED("UNAUTHORIZED", "未授权"), // 未授权状态码和描述

    /**
     * 禁止访问状态
     * 用于表示用户无权限访问该资源
     */
    FORBIDDEN("FORBIDDEN", "禁止访问"), // 禁止访问状态码和描述

    /**
     * 资源未找到状态
     * 用于表示请求的资源不存在
     */
    NOT_FOUND("NOT_FOUND", "资源未找到"), // 资源未找到状态码和描述

    /**
     * 服务器内部错误状态
     * 用于表示服务器处理请求时发生异常
     */
    INTERNAL_ERROR("INTERNAL_ERROR", "服务器内部错误"); // 服务器内部错误状态码和描述

    /**
     * 状态码
     * 用于标识返回结果的状态
     */
    private final String code; // 状态码字段，使用 final 保证不可变

    /**
     * 状态描述
     * 用于描述状态码的含义
     */
    private final String desc; // 状态描述字段，使用 final 保证不可变
}
