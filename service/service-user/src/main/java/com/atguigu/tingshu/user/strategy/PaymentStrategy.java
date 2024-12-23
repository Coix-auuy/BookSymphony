package com.atguigu.tingshu.user.strategy;

import com.atguigu.tingshu.vo.user.UserPaidRecordVo;

public interface PaymentStrategy {
    /**
     * 记录用户购买信息
     *
     * @param userPaidRecordVo
     */
    void processPayment(UserPaidRecordVo userPaidRecordVo);
}
