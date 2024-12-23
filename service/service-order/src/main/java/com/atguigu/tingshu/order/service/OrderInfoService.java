package com.atguigu.tingshu.order.service;

import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {


    /**
     * 订单结算页
     *
     * @param tradeVo
     * @param userId
     * @return
     */
    OrderInfoVo trade(TradeVo tradeVo, Long userId);

    /**
     * 提交订单
     *
     * @param orderInfoVo
     * @param userId
     * @return
     */
    String submitOrder(OrderInfoVo orderInfoVo, Long userId);

    /**
     * 订单未支付时，取消订单
     *
     * @param orderId
     */
    void orderCancel(long orderId);

    /**
     * 根据订单号获取订单信息
     *
     * @param orderNo
     * @return
     */
    OrderInfo getOrderInfo(String orderNo);

    /**
     * 根据状态{orderStatus：未支付、已支付、已取消}，查看我的订单，分页显示
     *
     * @param orderInfoPage
     * @param userId
     * @param orderStatus
     * @return
     */
    IPage<OrderInfo> findUserPage(Page<OrderInfo> orderInfoPage, Long userId, String orderStatus);

    /**
     * 订单支付成功，保存交易数据
     *
     * @param orderNo
     */
    void orderPaySuccess(String orderNo);
}
