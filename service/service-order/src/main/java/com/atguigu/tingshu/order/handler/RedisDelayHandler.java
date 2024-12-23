package com.atguigu.tingshu.order.handler;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBoundedBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Author HeZx
 * Time 2024/12/15 11:48
 * Version 1.0
 * Description:
 */
@Slf4j
@Component
public class RedisDelayHandler {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private OrderInfoService orderInfoService;

    // 开启一个线程 or 使用定时任务
    @PostConstruct
    // @PostConstruct 用于标记一个方法在依赖注入完成后执行。在这个例子中，init 方法会在 RedisDelayHandler 实例被创建并完成所有依赖注入后自动调用。这个方法通常用于初始化操作，比如启动线程或设置定时任务。
    public void init() {
        new Thread(() -> {
            while (true) {
                // 从队列中获取数据
                RBoundedBlockingQueue<Object> blockingQueue = redissonClient.getBoundedBlockingQueue(KafkaConstant.QUEUE_ORDER_CANCEL);
                try {
                    String orderId = (String) blockingQueue.take();
                    log.info("判断订单是否取消，订单号：{}", orderId);
                    orderInfoService.orderCancel(Long.parseLong(orderId));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
