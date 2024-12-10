package com.atguigu.tingshu.common.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author HeZx
 * Time 2024/12/3 10:49
 * Version 1.0
 * Description:
 */
@Target({ElementType.METHOD}) // 注解作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 注解的声明周期，RUNTIME 表示在整个 Java 运行时都生效
public @interface GuiGuCache {
    // 给注解添加一些属性
    // 保证锁的 key 不一致
    String prefix() default "cache:";

    // 后缀
    String suffix() default ":lock";
}
