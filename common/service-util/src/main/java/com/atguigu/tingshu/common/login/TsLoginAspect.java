package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import com.mysql.cj.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Author HeZx
 * Time 2024/10/18 10:34
 * Version 1.0
 * Description:
 */
@Component
@Aspect
public class TsLoginAspect {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 登录切面
     *
     * @param joinPoint
     * @param tsLogin
     * @return
     * @throws Throwable
     */
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(tsLogin)")
    // 匹配所有 com.atguigu.tingshu.* 包下的 api 包中的所有方法，并且带有 TsLogin 注解
    public Object tsLogin(ProceedingJoinPoint joinPoint, TsLogin tsLogin) throws Throwable {
        try {
        /*登录成功后，
            1. 请求会携带 token;
            2. 将用户信息存储到缓存 redis 中，用什么数据类型？
                用户信息不超过 512 m -> string：key: token, value: userInfo-序列化，否则选 hash（hash 也更便于修改）。
        */
            // 判断请求头中是否携带 token
            // 从 spring 容器 request 上下文请求对象中获取 RequestAttributes
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();
            // 获取 token
            String token = request.getHeader("token");
            // 根据 tsLogin 判断是否需要登录
            if (tsLogin.required()) {
                // 判断 token 是否为空
                if (StringUtils.isNullOrEmpty(token)) {
                    // 提示用户登录
                    throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
                } else {
                    // 判断 token 是否有效
                    // 利用 token 获取 redis 中的用户信息缓存
                    String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
                    UserInfo userInfo = (UserInfo) redisTemplate.opsForValue().get(loginKey);
                    if (null == userInfo) {
                        throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
                    }
                }
            }
            // 有些场景不需要用户登录也可以访问，但登录与没有登录显示的内容可能有区别，这时，可以根据 ThreadLocal 中有没有用户信息进行判断
            // 若登录成功，将 userId、userName 存入 ThreadLocal
            if (!StringUtils.isNullOrEmpty(token)) {
                // 利用 token 获取 redis 中缓存的用户信息
                String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
                UserInfo userInfo = (UserInfo) redisTemplate.opsForValue().get(loginKey);
                if (null != userInfo) {
                    AuthContextHolder.setUserId(userInfo.getId());
                    // AuthContextHolder.setUsername(userInfo.getNickname());
                }
            }
            // 执行带有注解的方法体（放行）
            return joinPoint.proceed();
        } finally {
            // 释放 ThreadLocal 中的数据
            AuthContextHolder.removeUserId();
        }
    }
}
