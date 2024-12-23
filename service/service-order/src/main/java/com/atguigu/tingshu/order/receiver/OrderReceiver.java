package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.mysql.cj.util.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Author HeZx
 * Time 2024/12/17 10:13
 * Version 1.0
 * Description:
 */

@Component
public class OrderReceiver {
    private final OrderInfoService orderInfoService;

    public OrderReceiver(OrderInfoService orderInfoService) {
        this.orderInfoService = orderInfoService;
    }

    /**
     * 监听订单支付成功，修改订单状态
     *
     * @param record
     */
    @KafkaListener(topics = KafkaConstant.QUEUE_ORDER_PAY_SUCCESS)
    public void OrderPaySuccess(ConsumerRecord<String, String> record) {
        String orderNo = record.value();
        if (!StringUtils.isNullOrEmpty(orderNo)) {
            orderInfoService.orderPaySuccess(orderNo);
        }
    }
}
