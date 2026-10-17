package com.worm.unified.exp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BaseException 测试：构造器默认错误码、get() 自供能力。
 *
 * 为什么：BaseException 既承担业务错误码载体又实现 Supplier 供函数式抛出，
 * 默认码与 cause 传递不能漂移 —— 干什么：逐构造器钉死 code / message / cause
 */
class BaseExceptionTest {

    @Test
    @DisplayName("单参构造：默认错误码 400")
    void singleArgConstructorDefaultsTo400() {
        BaseException exp = new BaseException("业务错误");

        assertThat(exp.getCode()).isEqualTo(400);
        assertThat(exp.getMessage()).isEqualTo("业务错误");
        assertThat(exp.getCause()).isNull();
    }

    @Test
    @DisplayName("双参构造：自定义错误码")
    void codeAndMessageConstructor() {
        BaseException exp = new BaseException(4001, "库存不足");

        assertThat(exp.getCode()).isEqualTo(4001);
        assertThat(exp.getMessage()).isEqualTo("库存不足");
    }

    @Test
    @DisplayName("带原因构造：默认错误码 400 且 cause 透传")
    void messageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("root");
        BaseException exp = new BaseException("包装异常", cause);

        assertThat(exp.getCode()).isEqualTo(400);
        assertThat(exp.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("完整构造：错误码、消息、原因全部透传")
    void fullConstructor() {
        RuntimeException cause = new RuntimeException("root");
        BaseException exp = new BaseException(4002, "完整异常", cause);

        assertThat(exp.getCode()).isEqualTo(4002);
        assertThat(exp.getMessage()).isEqualTo("完整异常");
        assertThat(exp.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("get()：Supplier 契约返回自身")
    void getReturnsSelf() {
        BaseException exp = new BaseException("业务错误");

        assertThat(exp.get()).isSameAs(exp);
    }
}
