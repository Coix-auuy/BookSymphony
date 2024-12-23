package com.atguigu.tingshu.user.factory;

import com.atguigu.tingshu.user.strategy.PaymentStrategy;

/**
 * Author HeZx
 * Time 2024/12/21 16:55
 * Version 1.0
 * Description:
 */
public interface StrategyFactory {
    /**
     * 根据字符串获取对应的策略对象
     * @param itemType
     * @return
     */
    PaymentStrategy getStrategy(String itemType);
}
