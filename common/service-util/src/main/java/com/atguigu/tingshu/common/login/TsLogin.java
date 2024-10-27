package com.atguigu.tingshu.common.login;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author HeZx
 * Time 2024/10/18 10:14
 * Version 1.0
 * Description: 登录校验
 */
@Target({ElementType.METHOD}) // 注解作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 注解的声明周期，RUNTIME 表示在整个 Java 运行时都生效
public @interface TsLogin {
    /**
     * true:表示需要登录 false:表示不需要登录
     *
     * @return
     */
    boolean required() default true;
}
