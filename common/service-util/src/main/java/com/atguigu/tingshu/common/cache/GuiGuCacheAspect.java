package com.atguigu.tingshu.common.cache;

import com.atguigu.tingshu.common.constant.RedisConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author HeZx
 * Time 2024/12/3 10:55
 * Version 1.0
 * Description:
 */
@Slf4j
@Aspect
@Component
public class GuiGuCacheAspect {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedisTemplate redisTemplate;

    // 编写切面类，使用环绕通知去处理
    // 实现 aop：1. 通过 xml 配置；2. 通过 @Aspect 注解实现
    @Around("@annotation(guiGuCache)")
    public Object guiGuCache(ProceedingJoinPoint joinPoint, GuiGuCache guiGuCache) throws Throwable {
        // 实现分布式锁的业务逻辑
        String prefix = guiGuCache.prefix();
        String suffix = guiGuCache.suffix();
        List<Object> list = Arrays.asList(joinPoint.getArgs());
        // 先查询缓存，缓存中没有才会查询数据库，走分布式业务逻辑
        String dataKey = prefix + list;
        try {
            Object object = redisTemplate.opsForValue().get(dataKey);
            if (null == object) {
                // 获取到锁
                String lockKey = prefix + list + suffix;
                RLock lock = redissonClient.getLock(lockKey);
                // 上锁
                boolean result = lock.tryLock(RedisConstant.ALBUM_LOCK_EXPIRE_PX1, RedisConstant.ALBUM_LOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                // lock 与 tryLock 的区别，tryLock 有最大等待时间
                // 判断 result
                if (result) {
                    try {
                        object = redisTemplate.opsForValue().get(dataKey);
                        if (null != object) {
                            return object;
                        }
                        // 获取到锁时，编写业务逻辑，执行带有注解的方法体
                        object = joinPoint.proceed();
                        if (null == object) {
                            // 存储一个空对象，防止缓存穿透
                            redisTemplate.opsForValue().set(dataKey, new Object(), RedisConstant.CACHE_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            return new Object();
                        }
                        // 存储真实数据
                        redisTemplate.opsForValue().set(dataKey, object, RedisConstant.CACHE_TIMEOUT, TimeUnit.SECONDS);
                        return object;
                    } finally {
                        lock.unlock();
                    }
                } else {
                    // 没拿到锁，自旋
                    guiGuCache(joinPoint, guiGuCache);
                }
            } else {
                // 返回缓存数据
                return object;
            }
        } catch (Throwable e) {
            log.error(" redis 服务器异常 {}", e.getMessage());
        }
        return joinPoint.proceed();
    }
}
