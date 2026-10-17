package com.worm.unified.exp;

import lombok.AllArgsConstructor; // Lombok 注解，自动生成全参构造函数
import lombok.Data; // Lombok 注解，自动生成 getter/setter/toString/equals/hashCode 方法
import lombok.EqualsAndHashCode; // Lombok 注解，自动生成 equals 和 hashCode 方法

import java.util.function.Supplier; // 函数式接口，用于延迟获取异常实例

/**
 * 基础异常类（非受检异常）
 * 继承 RuntimeException，使用时无需显式捕获或声明
 * 支持自定义错误码和异常信息
 *
 * @author wbyao
 */
@Data // Lombok 注解，自动生成 getter/setter 方法
@EqualsAndHashCode(callSuper = false) // 生成 equals 和 hashCode 方法，不调用父类的方法
@AllArgsConstructor // Lombok 注解，自动生成包含所有参数的构造函数
public class BaseException extends RuntimeException implements Supplier<BaseException> {

    /**
     * 错误码
     * 用于标识具体的错误类型，便于前端或调用方根据错误码进行业务处理
     */
    private final Integer code; // 错误码字段，使用 final 保证不可变

    /**
     * 实现 Supplier 接口的 get 方法
     * 用于支持函数式编程风格的异常获取
     *
     * @return 当前异常实例
     */
    @Override
    public BaseException get() {
        return this; // 返回当前异常实例
    }

    /**
     * 构造异常（默认错误码 400）
     * 适用于一般的业务异常场景
     *
     * @param message 异常信息，描述具体的错误原因
     */
    public BaseException(String message) {
        super(message); // 调用父类 RuntimeException 的构造函数，设置异常信息
        this.code = 400; // 设置默认错误码为 400（客户端错误）
    }

    /**
     * 构造异常（自定义错误码和消息）
     * 适用于需要特定错误码的业务场景
     *
     * @param code    错误码，用于标识具体的错误类型
     * @param message 异常信息，描述具体的错误原因
     */
    public BaseException(Integer code, String message) {
        super(message); // 调用父类 RuntimeException 的构造函数，设置异常信息
        this.code = code; // 设置自定义错误码
    }

    /**
     * 构造异常（带原因，默认错误码 400）
     * 适用于包装其他异常的场景
     *
     * @param message 异常信息，描述具体的错误原因
     * @param cause   原始异常，用于异常链追踪
     */
    public BaseException(String message, Throwable cause) {
        super(message, cause); // 调用父类构造函数，设置异常信息和原因
        this.code = 400; // 设置默认错误码为 400（客户端错误）
    }

    /**
     * 构造异常（完整参数：错误码、消息、原因）
     * 适用于需要完整异常信息的复杂场景
     *
     * @param code    错误码，用于标识具体的错误类型
     * @param message 异常信息，描述具体的错误原因
     * @param cause   原始异常，用于异常链追踪
     */
    public BaseException(Integer code, String message, Throwable cause) {
        super(message, cause); // 调用父类构造函数，设置异常信息和原因
        this.code = code; // 设置自定义错误码
    }
}
