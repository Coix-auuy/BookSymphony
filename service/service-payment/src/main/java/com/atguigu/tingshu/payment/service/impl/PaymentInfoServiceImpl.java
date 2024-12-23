package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.account.client.RechargeInfoFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;

    @Autowired
    private RechargeInfoFeignClient rechargeInfoFeignClient;
    @Autowired
    private KafkaService kafkaService;

    @Override
    public PaymentInfo savePaymentInfo(String paymentType, Long userId, String orderNo) {
        // 获取交易记录对象 防止重复保存
        PaymentInfo paymentInfo = getOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        if (null == paymentInfo) {
            paymentInfo = new PaymentInfo();
            paymentInfo.setUserId(userId);
            paymentInfo.setPaymentType(paymentType);
            paymentInfo.setOrderNo(orderNo);
            paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
            paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
            if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)) {
                // 保存订单交易记录
                Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderNo);
                Asserts.notNull(orderInfoResult, "订单信息结果集为空");
                OrderInfo orderInfo = orderInfoResult.getData();
                Asserts.notNull(orderInfo, "订单信息为空");
                paymentInfo.setContent("订单");
                paymentInfo.setAmount(orderInfo.getOrderAmount());
            } else if (SystemConstant.PAYMENT_TYPE_RECHARGE.equals(paymentType)) {
                // 保存充值交易记录
                Result<RechargeInfo> rechargeInfoResult = rechargeInfoFeignClient.getRechargeInfo(orderNo);
                Asserts.notNull(rechargeInfoResult, "充值信息结果集为空");
                RechargeInfo rechargeInfo = rechargeInfoResult.getData();
                Asserts.notNull(rechargeInfo, "充值信息为空");
                paymentInfo.setContent("充值");
                paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
            }
            save(paymentInfo);
        }
        return paymentInfo;
    }

    /**
     * 更新支付状态
     *
     * @param result
     */
    @Override
    public void updatePaymentStatus(Transaction result) {
        // 先修改 payment_info 表
        PaymentInfo paymentInfo = getOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, result.getOutTradeNo()));
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_PAID);
        // 设置微信的交易号
        paymentInfo.setOutTradeNo(result.getTransactionId());
        // 设置回调时间
        paymentInfo.setCallbackTime(new Date());
        // 设置回调内容
        paymentInfo.setCallbackContent(result.toString());
        save(paymentInfo);
        // 发送消息，修改订单状态或充值状态
        String topic = SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentInfo.getPaymentType()) ? KafkaConstant.QUEUE_ORDER_PAY_SUCCESS : KafkaConstant.QUEUE_RECHARGE_PAY_SUCCESS;
        kafkaService.sendMsg(topic, result.getOutTradeNo());
    }
}
