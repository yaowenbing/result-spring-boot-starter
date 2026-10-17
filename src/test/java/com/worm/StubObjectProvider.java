package com.worm;

import org.springframework.beans.factory.ObjectProvider;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * 测试用 ObjectProvider 桩：固定返回一个实例（可为 null）。
 *
 * 为什么：BaseResultHandler 经 ObjectProvider 延迟获取可选的 MessageSource，
 * 单测需要精确控制"消息源存在 / 缺失"两种场景 —— 干什么：以构造注入的实例充当提供者，null 表示容器中无该 Bean
 */
public class StubObjectProvider<T> implements ObjectProvider<T> {

    private final T instance;

    public StubObjectProvider(T instance) {
        this.instance = instance;
    }

    @Override
    public T getObject() {
        return instance;
    }

    @Override
    public T getObject(Object... args) {
        return instance;
    }

    @Override
    public T getIfAvailable() {
        return instance;
    }

    @Override
    public T getIfUnique() {
        return instance;
    }

    @Override
    public Stream<T> stream() {
        return instance == null ? Stream.empty() : Stream.of(instance);
    }

    @Override
    public Iterator<T> iterator() {
        return stream().iterator();
    }
}
