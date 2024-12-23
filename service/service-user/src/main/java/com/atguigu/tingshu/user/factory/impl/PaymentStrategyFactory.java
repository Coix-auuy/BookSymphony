package com.atguigu.tingshu.user.factory.impl;

import com.atguigu.tingshu.user.factory.StrategyFactory;
import com.atguigu.tingshu.user.strategy.PaymentStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentStrategyFactory implements StrategyFactory {

    // albumPaymentStrategy: AlbumPaymentStrategy
    // trackPaymentStrategy: TrackPaymentStrategy
    // vipPaymentStrategy: VipPaymentStrategy
    // 根据 Spring 容器中 PaymentStrategy 类型的 Bean 名称自动注入到 StrategyMap 中。
    // Spring 会扫描所有实现了 PaymentStrategy 接口的 Bean，并将它们以 Bean 名称为键，Bean 实例为值放入 Map 中。
    @Autowired
    private Map<String, PaymentStrategy> StrategyMap;

    @Override
    public PaymentStrategy getStrategy(String itemType) {
        // 获取策略对象
        PaymentStrategy strategy = StrategyMap.get(itemType);
        if (strategy == null) {
            throw new IllegalArgumentException("未知的支付类型: " + itemType);
        }
        return strategy;
    }
}
