package com.atguigu.tingshu.payment.service;

import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wechat.pay.java.service.payments.model.Transaction;

public interface PaymentInfoService extends IService<PaymentInfo> {
    /**
     * 保存交易记录到 payment_info 表
     *
     * @param paymentType
     * @param userId
     * @param orderNo
     * @return
     */
    PaymentInfo savePaymentInfo(String paymentType, Long userId, String orderNo);

    /**
     * 更新支付状态
     *
     * @param result
     */
    void updatePaymentStatus(Transaction result);
}
