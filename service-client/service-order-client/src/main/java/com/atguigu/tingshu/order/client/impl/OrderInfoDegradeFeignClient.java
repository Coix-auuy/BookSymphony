package com.atguigu.tingshu.order.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import org.springframework.stereotype.Component;

@Component
public class OrderInfoDegradeFeignClient implements OrderInfoFeignClient {

    @Override
    public Result<OrderInfo> getOrderInfo(String orderNo) {
        System.out.println("调用失败！");
        return null;
    }
}
