package com.atguigu.tingshu.order.service.impl;

import com.atguigu.tingshu.model.order.OrderDetail;
import com.atguigu.tingshu.order.mapper.OrderDetailMapper;
import com.atguigu.tingshu.order.service.OrderDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Author HeZx
 * Time 2024/12/14 10:17
 * Version 1.0
 * Description:
 */
@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
}
