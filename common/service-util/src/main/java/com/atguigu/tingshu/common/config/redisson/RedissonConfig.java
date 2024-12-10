package com.atguigu.tingshu.common.config.redisson;

import com.alibaba.cloud.commons.lang.StringUtils;
import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Author HeZx
 * Time 2024/12/1 8:48
 * Version 1.0
 * Description: Redisson 的配置信息
 */
@Data
@Configuration
/*
@ConfigurationProperties 注解是 Spring Boot 框架提供的一个功能强大的工具，主要用于将配置文件中的配置自动绑定到 Bean 的属性上。以下是其主要用途：
    配置绑定：可以从 application.properties 或 application.yml 文件中读取配置，并自动绑定到带有 @ConfigurationProperties 注解的类的属性上。
 */
@ConfigurationProperties("spring.data.redis")
public class RedissonConfig {
    private String host;

    private String password;

    private String port;

    private int timeout = 3000;
    private static String ADDRESS_PREFIX = "redis://";

    /**
     * 自动装配
     */
    @Bean
    RedissonClient redissonSingle() {
        Config config = new Config();

        if (StringUtils.isEmpty(host)) {
            throw new RuntimeException("host is  empty");
        }
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(ADDRESS_PREFIX + this.host + ":" + port)
                .setTimeout(this.timeout);
        if (!StringUtils.isEmpty(this.password)) {
            serverConfig.setPassword(this.password);
        }
        return Redisson.create(config);
    }
}
