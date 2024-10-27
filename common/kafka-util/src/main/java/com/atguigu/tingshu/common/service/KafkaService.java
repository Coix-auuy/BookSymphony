package com.atguigu.tingshu.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaService.class);

    @Autowired
    private KafkaTemplate kafkaTemplate;

    public boolean sendMsg(String topic, String value) {
        // 发送消息
        // CompletableFuture
        CompletableFuture completableFuture = kafkaTemplate.send(topic, value);
        completableFuture.thenAcceptAsync(o -> logger.info("消息发送成功")).exceptionally(throwable -> {
            logger.error("消息发送失败");
            return false;
        });
        return true;
        // 判断消息是否发送成功
    }
}
